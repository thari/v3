/*
 Copyright (c) 2016, Robby, Kansas State University
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

package org.sireum.awas.test.ast

import org.sireum.awas.ast.{PrettyPrinter, Builder}
import org.sireum.test._
import org.sireum.util._
import org.sireum.awas.test.parser.Antlr4AwasParserTestDefProvider._

object PrettyPrinterTestDefProvider {
}

final class PrettyPrinterTestDefProvider(tf: TestFramework)
  extends TestDefProvider {

  override def testDefs: ISeq[TestDef] = ivector(
    EqualOptTest("abcloop", printAndParse(abcloop), abcloop),
    EqualOptTest("properties", printAndParse(properties), properties),
    EqualOptTest("isolette_model", printAndParse(isolette_model), isolette_model),
    EqualOptTest("isolette_accident_level", printAndParse(isolette_accident_level), isolette_accident_level),
    EqualOptTest("isolette_accident", printAndParse(isolette_accident), isolette_accident),
    ConditionTest("PcaShutoff", Builder(printAndParse(pcashutOff).get).isDefined)
  )

  def printAndParse(model: String) : Option[String]= {
    Builder(model) match {
      case None => None
      case Some(e) =>
        val result = PrettyPrinter(e)
        Some(result)
    }
  }
}