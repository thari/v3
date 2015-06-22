/*
Copyright (c) 2015, Robby, Kansas State University
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

package org.sireum.pilar.parser

import org.sireum.test._
import utest._

object FastParserTestJs extends UTestTestFramework {

  def main(args: Array[String]): Unit = {
    generate()
  }

  override lazy val provider: TestDefProvider =
    new FastParserTestDefProvider(this)

  def tests = TestSuite {

    // This uTest list is auto-generated from data in
    // FastParserTestDefProvider

    "AnnRecovery" - {
      test("AnnRecovery")
    }

    "AnnRecoveryString" - {
      test("AnnRecoveryString")
    }

    "Annotation" - {
      test("Annotation")
    }

    "ComplexID" - {
      test("ComplexID")
    }

    "EmptyLit" - {
      test("EmptyLit")
    }

    "ExpComplexID" - {
      test("ExpComplexID")
    }

    "ExpInfixExp" - {
      test("ExpInfixExp")
    }

    "ExpLit" - {
      test("ExpLit")
    }

    "ExpOpID" - {
      test("ExpOpID")
    }

    "ExpPrefixExp" - {
      test("ExpPrefixExp")
    }

    "ExpPrefixRecovery" - {
      test("ExpPrefixRecovery")
    }

    "ExpSimpleID1" - {
      test("ExpSimpleID1")
    }

    "ExpSimpleID2" - {
      test("ExpSimpleID2")
    }

    "ExpSimpleIDNotEnd1" - {
      test("ExpSimpleIDNotEnd1")
    }

    "ExpSimpleIDNotEnd2" - {
      test("ExpSimpleIDNotEnd2")
    }

    "ExpTuple" - {
      test("ExpTuple")
    }

    "ExpTupleRecovery" - {
      test("ExpTupleRecovery")
    }

    "IDError" - {
      test("IDError")
    }

    "OpID" - {
      test("OpID")
    }

    "PrimaryExpComplexID" - {
      test("PrimaryExpComplexID")
    }

    "PrimaryExpIDError" - {
      test("PrimaryExpIDError")
    }

    "PrimaryExpLit" - {
      test("PrimaryExpLit")
    }

    "PrimaryExpOpID" - {
      test("PrimaryExpOpID")
    }

    "PrimaryExpSimpleID1" - {
      test("PrimaryExpSimpleID1")
    }

    "PrimaryExpSimpleID2" - {
      test("PrimaryExpSimpleID2")
    }

    "PrimaryExpSimpleIDNotEnd1" - {
      test("PrimaryExpSimpleIDNotEnd1")
    }

    "PrimaryExpSimpleIDNotEnd2" - {
      test("PrimaryExpSimpleIDNotEnd2")
    }

    "PrimaryExpTuple" - {
      test("PrimaryExpTuple")
    }

    "SimpleID1" - {
      test("SimpleID1")
    }

    "SimpleID2" - {
      test("SimpleID2")
    }

    "SimpleIDNotEnd1" - {
      test("SimpleIDNotEnd1")
    }

    "SimpleIDNotEnd2" - {
      test("SimpleIDNotEnd2")
    }

    "SimpleLit" - {
      test("SimpleLit")
    }

    "StringLit" - {
      test("StringLit")
    }

  }
}
