/*
Copyright (c) 2011-2015, Robby, Kansas State University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

import com.typesafe.sbteclipse.core.EclipsePlugin._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._
import sbt.complete.Parsers._
import sbtassembly.AssemblyPlugin
import sbtassembly.AssemblyPlugin._
import sbtassembly.AssemblyKeys._

object SireumBuild extends Build {
  final val isRelease = System.getenv("SIREUM_RELEASE") != null

  final val scalaVer = "2.11.6"

  final val sireumVer = "3.0-SNAPSHOT"

  final val BUILD_FILENAME = "BUILD"
  final val CORE_DIR = "core/"

  import ProjectInfo._

  val depDot = InputKey[Unit]("dep-dot", "Print project dependency in dot.")

  lazy val sireum = Project(
    id = "sireum",
    settings = sireumSettings,
    base = file(".")
  ).settings(name := "Sireum").
    aggregate(sireumJvm, sireumJs)

  lazy val sireumJvm =
    Project(
      id = "sireum-jvm",
      settings = sireumJvmSettings ++ assemblySettings ++
        Seq(
          name := "Sireum.jvm",
          assemblyJarName in assembly := "sireum-v3.jar",
          test in assembly := {},
          depDot := {
            val args = spaceDelimited("<arg>").parsed
            dotDependency(args)
          }),
      base = file("jvm")).
      aggregate(subProjectJvmReferences: _*).
      dependsOn(subProjectJvmClasspathDeps: _*)

  lazy val sireumJs =
    Project(
      id = "sireum-js",
      settings = sireumJsSettings ++
        Seq(
          name := "Sireum.js",
          depDot := {
            val args = spaceDelimited("<arg>").parsed
            dotDependency(args)
          }),
      base = file("js")).
      aggregate(subProjectJsReferences: _*).
      disablePlugins(AssemblyPlugin)

  lazy val subProjects = Seq(
    util, option, pilar,
    coreTest
  )

  lazy val subProjectsJvm = Seq(
    cli, pilarParserAntlr4, utilReflect
  )

  lazy val subProjectsJs = Seq(
    coreJsTest
  )

  lazy val subProjectJvmReferences =
    (subProjects ++ subProjectsJvm).map(x => x: ProjectReference)

  lazy val subProjectJvmClasspathDeps =
    (subProjects ++ subProjectsJvm).map(x => x: ClasspathDep[ProjectReference])


  lazy val subProjectJsReferences =
    subProjectsJs.map(x => x: ProjectReference)

  val sireumSettings = Seq(
    organization := "org.sireum",
    version := sireumVer,
    incOptions := incOptions.value.withNameHashing(true),
    scalaVersion := scalaVer,
    retrieveManaged := true,
    EclipseKeys.withSource := true,
    EclipseKeys.relativizeLibs := true,
    EclipseKeys.useProjectId := true,
    EclipseKeys.withBundledScalaContainers := true,
    EclipseKeys.eclipseOutput := Some("bin")
  )

  val sireumJvmSettings = sireumSettings ++ Seq(
    artifactName := { (config: ScalaVersion, module: ModuleID, artifact: Artifact) =>
      artifact.name + (
        artifact.classifier match {
          case Some("sources") => "-src"
          case Some("javadoc") => "-doc"
          case _ => ""
        }) + "." + artifact.extension
    },
    parallelExecution in Test := true,
    scalacOptions ++= (Seq("-target:jvm-1.8", "-Ybackend:GenBCode") ++
      (if (isRelease) Seq("-optimize", "-Yinline-warnings") else Seq())),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVer,
      "org.scala-lang" % "scala-compiler" % scalaVer,
      "com.lihaoyi" %% "upickle" % "0.2.8",
      "org.antlr" % "antlr4-runtime" % "4.5",
      "org.antlr" % "ST4" % "4.0.8"
    ),
    scalacOptions in(Compile, doc) := Seq("-groups", "-implicits"),
    javacOptions in(Compile, doc) := Seq("-notimestamp", "-linksource"),
    autoAPIMappings := true,
    apiURL := Some(url("http://sireum.org/api/"))
  )

  val sireumJvmTestSettings = sireumJvmSettings ++ Seq(
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v"),
    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % "test"
    )
  )

  val sireumJsSettings = sireumSettings ++ Seq(
    parallelExecution in Test := false,
    relativeSourceMaps := true,
    scalaJSStage in Global := (if (isRelease) FullOptStage else FastOptStage),
    postLinkJSEnv := NodeJSEnv().value,
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "upickle" % "0.2.8"
    )
  )

  val sireumJsTestSettings = sireumJsSettings ++ Seq(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "utest" % "0.3.1",
      "org.scala-lang" % "scala-reflect" % scalaVer
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

  // Cross Projects
  val utilPI = new ProjectInfo("sireum-util", CORE_DIR, Seq())
  val optionPI = new ProjectInfo("sireum-option", CORE_DIR, Seq(), utilPI)
  val pilarPI = new ProjectInfo("sireum-pilar", CORE_DIR, Seq(), utilPI)
  lazy val util = toSbtProject(utilPI, sireumJvmSettings)
  lazy val option = toSbtProject(optionPI, sireumJvmSettings)
  lazy val pilar = toSbtProject(pilarPI, sireumJvmSettings)


  // Jvm Projects
  val pilarParserAntlr4PI = new ProjectInfo("sireum-pilar-parser-antlr4", CORE_DIR, Seq(), utilPI, pilarPI)
  val cliPI = new ProjectInfo("sireum-cli", CORE_DIR, Seq(), utilPI, optionPI, pilarPI)
  val utilReflectPI = new ProjectInfo("sireum-util-reflect", CORE_DIR, Seq(), utilPI, optionPI, pilarPI, pilarParserAntlr4PI, cliPI)
  lazy val pilarParserAntlr4 = toSbtProject(pilarParserAntlr4PI, sireumJvmSettings)
  lazy val utilReflect = toSbtProject(utilReflectPI, sireumJvmSettings)
  lazy val cli = toSbtProject(cliPI, sireumJvmSettings)

  // Jvm Test Projects
  val coreTestPI = new ProjectInfo("sireum-core-test", CORE_DIR, Seq(), utilPI, pilarPI, pilarParserAntlr4PI, optionPI, cliPI)
  lazy val coreTest = toSbtProject(coreTestPI, sireumJvmTestSettings)

  // Js Projects

  // Js Test Projects
  val coreJsTestPI = new ProjectInfo("sireum-core-js-test", CORE_DIR, Seq(), utilPI, pilarPI, coreTestPI)
  lazy val coreJsTest = toSbtJsProject(coreJsTestPI, sireumJsTestSettings)


  def toSbtProject(pi: ProjectInfo, settings: Seq[Def.Setting[_]]): Project =
    Project(
      id = pi.id,
      settings = settings,
      base = pi.baseDir).
      dependsOn(pi.dependencies.map { p =>
      new ClasspathDependency(new LocalProject(p.id), None)
    }: _*).
      settings(name := pi.name).disablePlugins(AssemblyPlugin)

  def toSbtJsProject(pi: ProjectInfo, settings: Seq[Def.Setting[_]]): Project = {
    val (jsPIs, purePIs) = pi.dependencies.partition(p => p.id.contains("-js"))
    Project(
      id = pi.id,
      settings = settings ++ Seq(
        name := pi.name,
        unmanagedSourceDirectories in Compile <++= baseDirectory { base =>
          purePIs.map { pi =>
            base / base.getAbsoluteFile.toPath.relativize((pi.baseDir / "src/main/scala").
              getAbsoluteFile.toPath).toString
          }
        }
      ),
      base = pi.baseDir).dependsOn(jsPIs.map(p =>
      new ClasspathDependency(new LocalProject(p.id), None)): _*
      ).enablePlugins(ScalaJSPlugin).disablePlugins(AssemblyPlugin)
  }
}
