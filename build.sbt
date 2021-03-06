/*
 Copyright (c) 2017, Robby, Kansas State University
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

import ProjectInfo._
import org.scalajs.sbtplugin.cross.CrossProject
import sbt.complete.Parsers.spaceDelimited
import sbtassembly.AssemblyKeys.{assemblyMergeStrategy, assemblyOutputPath}
import sbtassembly.{AssemblyUtils, MergeStrategy}
import sbtassembly.AssemblyPlugin._

val isRelease = System.getenv("SIREUM_RELEASE") != null

lazy val properties = {
  def findPropFile(): String = {
    def err(): Nothing = {
      throw new Error("Need to supply property 'org.sireum.version.file', property 'org.sireum.home', or 'SIREUM_HOME' env var.")
    }
    def checkFile(f: File): String = {
      if (f.isFile && f.canRead) return f.getCanonicalFile.getAbsolutePath
      err()
    }
    val propFile = System.getProperty("org.sireum.version.file")
    if (propFile == null) {
      var sireumHome = System.getProperty("org.sireum.home")
      if (sireumHome != null) {
        return checkFile(new File(sireumHome, "versions.properties"))
      }
      sireumHome = System.getenv("SIREUM_HOME")
      if (sireumHome != null) {
        return checkFile(new File(sireumHome, "versions.properties"))
      }
      err()
    } else {
      checkFile(new File(propFile))
    }
  }
  val propFile = findPropFile()
  println(s"[info] Loading Sireum dependency versions from $propFile ...")
  val ps = new _root_.java.util.Properties()
  IO.load(ps, new File(propFile))
  ps
}

def property(key: String): String = {
  val value = properties.getProperty(key)
  if (value == null) {
    throw new Error(s"Need to supply property '$key'.")
  }
  value
}

val sireumVersion = "3"

lazy val scalaVer = property("org.sireum.version.scala")

lazy val sireumScalacVersion = property("org.sireum.version.scalac-plugin")

lazy val metaVersion = property("org.sireum.version.scalameta")

lazy val scalaTestVersion = property("org.sireum.version.scalatest")

lazy val scalaJsDomVersion = property("org.sireum.version.scalajsdom")

lazy val scalaJsJQueryVersion = property("org.sireum.version.scalajsjquery")

lazy val scalaTagsVersion = property("org.sireum.version.scalatags")

lazy val parboiled2Version = property("org.sireum.version.parboiled2")

lazy val asmVersion = property("org.sireum.version.asm")

lazy val jgraphtVersion = property("org.sireum.version.jgrapht")

lazy val upickleVersion = property("org.sireum.version.upickle")

lazy val java8CompatVersion = property("org.sireum.version.java8compat")

lazy val antlrRuntimeVersion = property("org.sireum.version.antlr")

lazy val stVersion = property("org.sireum.version.stringtemplate")

lazy val ammoniteOpsVersion = property("org.sireum.version.ammonite-ops")

lazy val diffVersion = property("org.sireum.version.diff")

lazy val snakeYamlVersion = property("org.sireum.version.snakeyaml")

lazy val junitInterfaceVersion = property("org.sireum.version.junit-interface")

lazy val utestVersion = property("org.sireum.version.utest")

lazy val spireVersion = property("org.sireum.version.spire")

lazy val nuprocessVersion = property("org.sireum.version.nuprocess")

val BUILD_FILENAME = "BUILD"

val isParallelBuild = "false" != System.getenv("SIREUM_PARALLEL_BUILD")

val distros = TaskKey[Unit]("distros", "Build Sireum distributions.")
val iveDistros = TaskKey[Unit]("ive-distros", "Build Sireum IVE distributions.")
val devDistros = TaskKey[Unit]("dev-distros", "Build Sireum-dev distributions.")
val devIveDistros = TaskKey[Unit]("dev-ive-distros", "Build Sireum-dev IVE distributions.")
val depDot = InputKey[Unit]("dep-dot", "Print project dependency in dot.")
val refreshSlang = TaskKey[Unit]("refresh-slang", "Refresh Slang files.")

iveDistros / traceLevel := -1
Global / parallelExecution := isParallelBuild
Global / concurrentRestrictions ++= (if (isParallelBuild) Seq() else Seq(Tags.limitAll(1)))

addCommandAlias("refresh-slang", "; project sireum; refreshSlang")
addCommandAlias("fatjar", "; project sireum; assembly")

lazy val sireumSettings = Seq(
  organization := "org.sireum",
  version := sireumVersion,
  incOptions := incOptions.value.withLogRecompileOnMacro(false),
  scalaVersion := scalaVer,
  retrieveManaged := true,
  scalacOptions ++= Seq(
    "-target:jvm-1.8",
    "-deprecation",
    "-Ydelambdafy:method",
    "-feature",
    "-unchecked",
    "-Xfatal-warnings"
  ),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-encoding", "utf8"),
  Compile / doc / javacOptions := Seq("-notimestamp", "-linksource"),
  Compile / doc / scalacOptions := Seq("-groups", "-implicits"),
  Test / logBuffered := false,
  autoAPIMappings := true,
  apiURL := Some(url("http://v3.sireum.org/api/")),
  resolvers += Resolver.sonatypeRepo("public"),
  dependencyUpdatesFilter -= moduleFilter(organization = "com.lihaoyi", name = "upickle"),
  dependencyUpdatesFilter -= moduleFilter(organization = "org.scalatest"),
  dependencyUpdatesFilter -= moduleFilter(organization = "org.eclipse.jetty")
)

lazy val sireumSharedSettings = sireumSettings ++ Seq(
  libraryDependencies ++= Seq("com.lihaoyi" %%% "upickle" % upickleVersion)
)

lazy val sireumJvmSettings = sireumSharedSettings ++ Seq(
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVer,
    "org.scala-lang" % "scala-compiler" % scalaVer,
    "org.scala-lang.modules" %% "scala-java8-compat" % java8CompatVersion,
    "org.antlr" % "antlr4-runtime" % antlrRuntimeVersion,
    "com.zaxxer" % "nuprocess" % nuprocessVersion,
    "org.antlr" % "ST4" % stVersion,
    "org.yaml" % "snakeyaml" % snakeYamlVersion,
    "org.ow2.asm" % "asm" % asmVersion,
    "org.ow2.asm" % "asm-commons" % asmVersion,
    "org.ow2.asm" % "asm-util" % asmVersion,
    "org.jgrapht" % "jgrapht-core" % jgraphtVersion,
    "org.jgrapht" % "jgrapht-io" % jgraphtVersion,
    "com.lihaoyi" %% "ammonite-ops" % ammoniteOpsVersion,
    "com.sksamuel.diff" % "diff" % diffVersion,
    "com.novocode" % "junit-interface" % junitInterfaceVersion
  ),
  testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v")
)

lazy val sireumJsSettings = sireumSharedSettings ++ Seq(
  scalacOptions ++= Seq("-feature"),
  relativeSourceMaps := true,
  Global / scalaJSStage := (if (isRelease) FullOptStage else FastOptStage),
  libraryDependencies ++= Seq("org.scala-lang" % "scala-reflect" % scalaVer, "com.lihaoyi" %%% "utest" % utestVersion),
  testFrameworks += new TestFramework("utest.runner.Framework")
)

lazy val webSettings = sireumSettings ++ Seq(
  Compile / fastOptJS / crossTarget := (Compile / classDirectory).value,
  Compile / packageJSDependencies / crossTarget := (Compile / classDirectory).value,
  Compile / fullOptJS / crossTarget := (Compile / classDirectory).value / "min",
  Compile / packageMinifiedJSDependencies / crossTarget := (Compile / classDirectory).value / "min",
  packageJSDependencies / skip := false,
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % scalaJsDomVersion,
    "be.doeraene" %%% "scalajs-jquery" % scalaJsJQueryVersion,
    "com.lihaoyi" %%% "scalatags" % scalaTagsVersion
  )
)

lazy val commonSlangSettings = Seq(addCompilerPlugin("org.sireum" %% "scalac-plugin" % sireumScalacVersion))

lazy val slangSettings = sireumSettings ++ commonSlangSettings ++ Seq(scalacOptions ++= Seq("-Yrangepos"))

val depOpt = Some("test->test;compile->compile;test->compile")

def toSbtJvmProject(pi: ProjectInfo, settings: Seq[Def.Setting[_]] = sireumJvmSettings): Project =
  Project(id = pi.id, base = pi.baseDir / "jvm")
    .settings(Seq(name := pi.name) ++ settings: _*)
    .dependsOn(pi.dependencies.flatMap { p =>
      if (p.isCross)
        Seq(ClasspathDependency(LocalProject(p.id), depOpt), ClasspathDependency(LocalProject(p.id + "-jvm"), depOpt))
      else Seq(ClasspathDependency(LocalProject(p.id), depOpt))
    }: _*)
    .settings()
    .disablePlugins(AssemblyPlugin)

def toSbtCrossProject(pi: ProjectInfo, settings: Seq[Def.Setting[_]] = Vector()): (Project, Project, Project) = {
  val shared = Project(id = pi.id, base = pi.baseDir / "shared")
    .settings(Seq(name := pi.name) ++ sireumSharedSettings ++ settings: _*)
    .dependsOn(pi.dependencies.map { p =>
      ClasspathDependency(LocalProject(p.id), depOpt)
    }: _*)
    .disablePlugins(AssemblyPlugin)
  val cp = CrossProject(jvmId = pi.id + "-jvm", jsId = pi.id + "-js", base = pi.baseDir, crossType = CrossType.Full)
    .settings(name := pi.id)
  val jvm =
    cp.jvm
      .settings(sireumJvmSettings ++ settings)
      .disablePlugins(AssemblyPlugin)
      .dependsOn(shared % depOpt.get)
      .dependsOn(pi.dependencies.map { p =>
        ClasspathDependency(LocalProject(p.id), depOpt)
      }: _*)
      .dependsOn(pi.dependencies.map { p =>
        ClasspathDependency(LocalProject(p.id + "-jvm"), depOpt)
      }: _*)
  val js =
    cp.js
      .settings(sireumJsSettings ++ settings)
      .disablePlugins(AssemblyPlugin)
      .dependsOn(shared % depOpt.get)
      .dependsOn(pi.dependencies.map { p =>
        ClasspathDependency(LocalProject(p.id), depOpt)
      }: _*)
      .dependsOn(pi.dependencies.map { p =>
        ClasspathDependency(LocalProject(p.id + "-js"), depOpt)
      }: _*)
      .enablePlugins(ScalaJSPlugin)
  (shared, jvm, js)
}

// Cross Projects
lazy val utilPI = new ProjectInfo("util", isCross = true)
lazy val utilT = toSbtCrossProject(utilPI)
lazy val utilShared = utilT._1
lazy val utilJvm = utilT._2
lazy val utilJs = utilT._3.settings(webSettings: _*)

lazy val testPI = new ProjectInfo("test", isCross = true, utilPI)
lazy val testT = toSbtCrossProject(testPI)
lazy val testShared = testT._1
lazy val testJvm = testT._2
lazy val testJs = testT._3

lazy val pilarPI = new ProjectInfo("pilar", isCross = true, utilPI, testPI)
lazy val pilarT = toSbtCrossProject(pilarPI)
lazy val pilarShared = pilarT._1
lazy val pilarJvm = pilarT._2
lazy val pilarJs = pilarT._3

lazy val logikaPI = new ProjectInfo("logika", isCross = true, utilPI, testPI)
lazy val logikaT = toSbtCrossProject(logikaPI)
lazy val logikaShared = logikaT._1
lazy val logikaJvm = logikaT._2
  .settings(
    Compile / unmanagedResourceDirectories ++= Seq(
      logikaT._2.base / "c-runtime" / "include",
      logikaT._2.base / "c-runtime" / "src",
      logikaT._2.base / "c-runtime" / "cmake"
    )
  )
  .dependsOn(macrosJvm, libraryJvm)
lazy val logikaJs = logikaT._3

lazy val macrosPI = new ProjectInfo("runtime/macros", isCross = true)
lazy val macrosT = toSbtCrossProject(
  macrosPI,
  Seq(
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-l", "SireumRuntime"),
    libraryDependencies ++= Seq("org.scala-lang" % "scala-reflect" % scalaVer)
  )
)
lazy val macrosShared = macrosT._1
lazy val macrosJvm = macrosT._2
lazy val macrosJs = macrosT._3

lazy val libraryPI = new ProjectInfo("runtime/library", isCross = true, macrosPI)
lazy val libraryT = toSbtCrossProject(
  libraryPI,
  slangSettings ++ Seq(
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-l", "SireumRuntime"),
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % scalaTestVersion % "test",
      "org.spire-math" %%% "spire" % spireVersion % "test"
    )
  )
)
lazy val libraryShared = libraryT._1
lazy val libraryJvm = libraryT._2
lazy val libraryJs = libraryT._3

lazy val slangAstPI = new ProjectInfo("slang/ast", isCross = true, macrosPI, libraryPI)
lazy val slangAstT = toSbtCrossProject(slangAstPI, slangSettings)
lazy val slangAstShared = slangAstT._1
lazy val slangAstJvm = slangAstT._2
lazy val slangAstJs = slangAstT._3

lazy val slangParserPI = new ProjectInfo("slang/parser", isCross = true, macrosPI, libraryPI, slangAstPI)
lazy val slangParserT = toSbtCrossProject(
  slangParserPI,
  slangSettings ++ Seq(libraryDependencies ++= Seq("org.scalameta" %%% "scalameta" % metaVersion))
)
lazy val slangParserShared = slangParserT._1
lazy val slangParserJvm = slangParserT._2
lazy val slangParserJs = slangParserT._3

lazy val slangTipePI = new ProjectInfo("slang/tipe", isCross = true, macrosPI, libraryPI, slangAstPI)
lazy val slangTipeT = toSbtCrossProject(slangTipePI, slangSettings)
lazy val slangTipeShared = slangTipeT._1
lazy val slangTipeJvm = slangTipeT._2
lazy val slangTipeJs = slangTipeT._3

lazy val slangFrontEndPI = new ProjectInfo("slang/frontend", isCross = true, macrosPI, libraryPI, slangAstPI, slangParserPI, slangTipePI)
lazy val slangFrontEndT = toSbtCrossProject(
  slangFrontEndPI,
  slangSettings ++ Seq(libraryDependencies ++= Seq("org.scalatest" %%% "scalatest" % scalaTestVersion % "test"))
)
lazy val slangFrontEndShared = slangFrontEndT._1
lazy val slangFrontEndJvm = slangFrontEndT._2
lazy val slangFrontEndJs = slangFrontEndT._3

lazy val toolsPI = new ProjectInfo("tools", isCross = true, macrosPI, libraryPI, slangAstPI, slangParserPI, slangTipePI, slangFrontEndPI)
lazy val toolsT = toSbtCrossProject(
  toolsPI,
  slangSettings ++ Seq(libraryDependencies ++= Seq("org.scalatest" %%% "scalatest" % scalaTestVersion % "test"))
)
lazy val toolsShared = toolsT._1
lazy val toolsJvm = toolsT._2
lazy val toolsJs = toolsT._3

lazy val webPI = new ProjectInfo("web", isCross = true, macrosPI, libraryPI, utilPI)
lazy val webT = toSbtCrossProject(
  webPI,
  slangSettings ++ Seq(libraryDependencies ++= Seq("org.scalatest" %%% "scalatest" % scalaTestVersion % "test"))
)
lazy val webShared = webT._1
lazy val webJvm = webT._2
lazy val webJs = webT._3.settings(webSettings: _*)

lazy val airPI = new ProjectInfo("aadl/ir", isCross = true, utilPI, testPI, macrosPI, libraryPI)
lazy val airT = toSbtCrossProject(
  airPI,
  slangSettings ++ Seq(libraryDependencies ++= Seq("org.scalatest" %%% "scalatest" % scalaTestVersion % "test"))
)
lazy val airShared = airT._1
lazy val airJvm = airT._2
lazy val airJs = airT._3

// Jvm Projects

lazy val javaPI = new ProjectInfo("java", isCross = false, utilPI, testPI, pilarPI)
lazy val java = toSbtJvmProject(javaPI)

lazy val cliPI = new ProjectInfo("cli", isCross = false, utilPI, testPI, pilarPI, javaPI, logikaPI, toolsPI)
lazy val cli = toSbtJvmProject(cliPI, sireumJvmSettings ++ commonSlangSettings)

lazy val awasPI = new ProjectInfo("awas", isCross = true, utilPI, testPI, airPI)
lazy val awasT = toSbtCrossProject(awasPI, Seq(
  Test / parallelExecution := false,
  libraryDependencies ++= Seq(
    "com.lihaoyi" %%% "scalatags" % scalaTagsVersion,
    "org.parboiled" %% "parboiled" % parboiled2Version
  )))

lazy val awasShared = awasT._1
lazy val awasJvm = awasT._2
lazy val awasJs = awasT._3.settings(webSettings: _*)

lazy val arsitPI =
  new ProjectInfo("aadl/arsit", isCross = false, utilPI, testPI, macrosPI, libraryPI, airPI)
lazy val arsit = toSbtJvmProject(arsitPI, slangSettings)

lazy val minixPI = new ProjectInfo("aadl/minix", isCross = false, macrosPI, libraryPI, airPI)
lazy val minix = toSbtJvmProject(minixPI, slangSettings)

lazy val subProjectsJvm = Seq(
  utilJvm,
  testJvm,
  pilarJvm,
  macrosJvm,
  libraryJvm,
  logikaJvm,
  slangAstJvm,
  slangParserJvm,
  slangTipeJvm,
  slangFrontEndJvm,
  toolsJvm,
  java,
  cli,
  awasJvm,
  airJvm,
  arsit,
  minix
)

// Js Projects

lazy val subProjectsJs = Seq(
  utilJs,
  testJs,
  pilarJs,
  macrosJs,
  libraryJs,
  logikaJs,
  slangAstJs,
  slangParserJs,
  slangTipeJs,
  slangFrontEndJs,
  toolsJs,
  awasJs,
  airJs
)

lazy val subProjectJvmReferences =
  subProjectsJvm.map(x => x: ProjectReference)

lazy val subProjectJvmClasspathDeps =
  subProjectsJvm.map(x => x: ClasspathDep[ProjectReference])

lazy val subProjectJsReferences =
  subProjectsJs.map(x => x: ProjectReference)

lazy val sireumJvm =
  Project(id = "sireum-jvm", base = file("jvm"))
    .settings(
      sireumJvmSettings ++ assemblySettings ++
        Seq(
          name := "Sireum.jvm",
          libraryDependencies += "org.sireum" %% "scalac-plugin" % sireumScalacVersion,
          assembly / mainClass := Some("org.sireum.cli.Sireum"),
          assembly / assemblyOutputPath := file("bin/sireum.jar"),
          assembly / test := {},
          assembly / logLevel := Level.Error,
          assemblyExcludedJars in assembly := {
            val cp = (fullClasspath in assembly).value
            cp filter { x =>
              x.data.getName.contains("scalapb") || x.data.getName.contains("protobuf")
            }
          },
          assemblyShadeRules in assembly := Seq(
            ShadeRule.rename("com.novocode.**" -> "sh4d3.com.novocode.@1").inAll,
            ShadeRule.rename("com.sksamuel.**" -> "sh4d3.com.sksamuel.@1").inAll,
            ShadeRule.rename("com.trueaccord.**" -> "sh4d3.com.trueaccord.@1").inAll,
            ShadeRule.rename("com.zaxxer.**" -> "sh4d3.com.zaxxer.@1").inAll,
            ShadeRule.rename("fastparse.**" -> "sh4d3.fastparse.@1").inAll,
            ShadeRule.rename("google.**" -> "sh4d3.google.@1").inAll,
            ShadeRule.rename("org.langmeta.**" -> "sh4d3.org.langmeta.@1").inAll,
            ShadeRule.rename("org.scalameta.**" -> "sh4d3.org.scalameta.@1").inAll,
            ShadeRule.rename("scala.meta.**" -> "sh4d3.scala.meta.@1").inAll,
            ShadeRule.rename("scalapb.**" -> "sh4d3.scalapb.@1").inAll,
            ShadeRule.rename("upickle.**" -> "sh4d3.upickle.@1").inAll,
            ShadeRule.rename("sbt.**" -> "sh4d3.sbt.@1").inAll,
            ShadeRule.rename("org.yaml.**" -> "sh4d3.org.yaml.@1").inAll,
            ShadeRule.rename("org.stringtemplate.**" -> "sh4d3.org.stringtemplate.@1").inAll,
            ShadeRule.rename("org.objectweb.**" -> "sh4d3.org.objectweb.@1").inAll,
            ShadeRule.rename("org.junit.**" -> "sh4d3.org.junit.@1").inAll,
            ShadeRule.rename("org.jgrapht.**" -> "sh4d3.org.jgrapht.@1").inAll,
            ShadeRule.rename("org.hamcrest.**" -> "sh4d3.org.hamcrest.@1").inAll,
            ShadeRule.rename("org.apache.**" -> "sh4d3.org.apache.@1").inAll,
            ShadeRule.rename("org.antlr.**" -> "sh4d3.org.antlr.@1").inAll,
            ShadeRule.rename("org.scalatools.**" -> "sh4d3.org.scalatools.@1").inAll,
            ShadeRule.rename("machinist.**" -> "sh4d3.machinist.@1").inAll,
            ShadeRule.rename("junit.**" -> "sh4d3.junit.@1").inAll,
            ShadeRule.rename("jawn.**" -> "sh4d3.jawn.@1").inAll,
            ShadeRule.rename("geny.**" -> "sh4d3.geny.@1").inAll,
            ShadeRule.rename("ammonite.**" -> "sh4d3.ammonite.@1").inAll,
            ShadeRule.rename("scalatags.**" -> "sh4d3.scalatags.@1").inAll,
            ShadeRule.rename("sourcecode.**" -> "sh4d3.sourcecode.@1").inAll
          ),
          assembly / assemblyMergeStrategy := {
            case PathList("transformed", _*) => MergeStrategy.discard
            case PathList("Scratch.class") => MergeStrategy.discard
            case PathList("Scratch$.class") => MergeStrategy.discard
            case PathList("Scratch$delayedInit$body.class") => MergeStrategy.discard
            case PathList("sh4d3", "scala", "meta", _*) => MergeStrategy.first
            case PathList("org", "sireum", _*) =>
              new MergeStrategy {
                override def name: String = "sireum"

                override def apply(
                  tempDir: File,
                  path: String,
                  files: Seq[File]
                ): Either[String, Seq[(File, String)]] = {
                  if (files.size == 1) return Right(Seq(files.head -> path))
                  val nonSharedFiles =
                    files.flatMap { f =>
                      val sourceDir = AssemblyUtils.sourceOfFileForMerge(tempDir, f)._1
                      if (sourceDir.getAbsolutePath.contains("/shared/")) None else Some(f)
                    }
                  Right(Seq(nonSharedFiles.head -> path))
                }
              }
            case "module-info.class" => MergeStrategy.discard
            case x =>
              val oldStrategy = (assembly / assemblyMergeStrategy).value
              oldStrategy(x)
          }
        ): _*
    )
    .aggregate(subProjectJvmReferences: _*)
    .dependsOn(subProjectJvmClasspathDeps: _*)

lazy val sireumJs =
  Project(id = "sireum-js", base = file("js"))
    .settings(
      sireumSharedSettings ++
        Seq(name := "Sireum.js"): _*
    )
    .aggregate(subProjectJsReferences: _*)
    .disablePlugins(AssemblyPlugin)

lazy val sireum = Project(id = "sireum", base = file("."))
  .settings(
    sireumSharedSettings ++ Seq(
      name := "Sireum",
      distros := {
        Distros.build()
      },
      iveDistros := {
        Distros.buildIVE()
      },
      devDistros := {
        Distros.buildDev()
      },
      devIveDistros := {
        Distros.buildIVEDev()
      },
      depDot := {
        val args = spaceDelimited("<arg>").parsed
        dotDependency(args)
      },
      refreshSlang := {
        import ammonite.ops._
        val rootDir = baseDirectory.value
        val runtimeFile =
          Path(new File(rootDir, "runtime/library/shared/src/main/scala/org/sireum/Library_Ext.scala").getCanonicalFile)
        val slangFile =
          Path(new File(rootDir, "slang/frontend/shared/src/main/scala/org/sireum/lang/$SlangFiles.scala").getCanonicalFile)

        def touche(p: Path): Unit = {
          val text = read ! p
          if (text.last == '\n') {
            write.over(p, text.trim)
          } else {
            write.over(p, text + '\n')
          }
        }

        touche(runtimeFile)
        touche(slangFile)
      },
      initialize := {
        val required = Set("1.8", "9", "10")
        val current = sys.props("java.specification.version")
        assert(required.contains(current), s"Unsupported Java version: $current (required: $required)")
      },
      publish := {},
      publishLocal := {}
    ): _*
  )
  .aggregate(sireumJvm, sireumJs)
  .disablePlugins(AssemblyPlugin)
