/*
 * Copyright (c) 2018. Yuriy Stul
 */

/**
  * @author Yuriy Stul
  */
package com.stulsoft.csvreader

import org.scalatest.{FunSuite, Matchers}

class CsvReaderTest extends FunSuite with Matchers {

  //  test("testParseSource") {
  //    CsvReader.parseSource[TestData1](null)
  //  }

  test("parseLine") {
    val i = 123
    val s = "some text"
    val d = 321.98
    val testData1 = CsvReader.parseLine[TestData1](Seq(i.toString, s, d.toString))
    testData1 shouldBe TestData1(i, s, d)
  }
}
