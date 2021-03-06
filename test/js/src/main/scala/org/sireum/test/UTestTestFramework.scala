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

package org.sireum.test

import utest.TestSuite

import org.sireum.util._

abstract class UTestTestFramework
  extends TestSuite with TestFramework {

  def provider: TestDefProvider

  lazy val (testDefMap, single) = {
    var m = imapEmpty[String, TestDef]
    for (td <- provider.testDefs) {
      m = m + (td.name -> td)
    }
    (m, m.values.exists(_.isSingle))
  }

  def generate(): Unit = {
    for (name <- testDefMap.keys.toSeq.sorted) {
      println( s"""  "$name"-{ test("$name") }""")
      println()
    }
  }

  protected def test(name: String) = {
    val td = testDefMap(name)
    if (single) {
      if (td.isSingle) td.test(this)
    } else {
      td.test(this)
    }
  }

  override def assertEquals(expected: Any, result: Any): Unit =
    assert(expected == result)

  override def assertEqualsRaw(expected: Any, result: Any): Unit =
    assert(expected == result)

  override def assertEmpty(it: Iterable[_]): Unit =
    assert(it.isEmpty)

  override def assertTrue(b: Boolean): Unit =
    assert(b)
}