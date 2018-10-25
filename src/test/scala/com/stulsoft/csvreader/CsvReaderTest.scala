/*
 * Copyright (c) 2018. Yuriy Stul
 */

/**
  * @author Yuriy Stul
  */
package com.stulsoft.csvreader

import org.scalatest.{FunSuite, Matchers}

import scala.io.Source
import scala.util.{Failure, Success}

class CsvReaderTest extends FunSuite with Matchers {

  //  test("testParseSource") {
  //    CsvReader.parseSource[TestData1](null)
  //  }

  test("parseLine") {
    val i = 123
    val s = "some text"
    val d = 321.98
    CsvReader.parseLine[TestData1](Seq(i.toString, s, d.toString)) match {
      case Success(testData1) =>
        testData1 shouldBe TestData1(i, s, d)
      case Failure(exception) =>
        fail(exception.getMessage)
    }
  }

  test("parseLine with error") {
    val i = 123
    val s = "some text"
    CsvReader.parseLine[TestData1](Seq(i.toString, s)) match {
      case Success(testData1) =>
        fail("Exception should be thrown")
      case Failure(exception) =>
        succeed
    }
  }

  test("parseSource") {
    def handler(testData: TestData1): Unit = {
      println(testData)
    }

    CsvReader.parseSource[TestData1](Source.fromResource("test-data1.csv"), handler, err => println(s"ERROR: $err"))
  }
}
