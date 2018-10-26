/*
 * Copyright (c) 2018. Yuriy Stul
 */

/**
  * @author Yuriy Stul
  */
package com.stulsoft.csvreader

import org.scalatest.{FunSuite, Ignore, Matchers}

import scala.io.Source
import scala.util.{Failure, Success}

class CsvReaderTest extends FunSuite with Matchers {

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


  test("parseLine with empty option parameter") {
    val i = 123
    val d = 321.98
    CsvReader.parseLine[TestData2](Seq(i.toString, "", d.toString)) match {
      case Success(testData2) =>
        testData2 shouldBe TestData2(i, None, d)
      case Failure(exception) =>
        fail(exception.getMessage)
    }
  }


  test("parseLine with non empty option parameter") {
    val i = 123
    val s = "the text"
    val d = 321.98
    CsvReader.parseLine[TestData2](Seq(i.toString, s, d.toString)) match {
      case Success(testData2) =>
        testData2 shouldBe TestData2(i, Some(s), d)
      case Failure(exception) =>
        fail(exception.getMessage)
    }
  }

  test("parseLine all empty") {
    CsvReader.parseLine[TestData3](Seq("", "", "")) match {
      case Success(testData3) =>
        testData3 shouldBe TestData3(None, None, None)
      case Failure(exception) =>
        fail(exception.getMessage)
    }
  }

  test("parseLine all defined") {
    CsvReader.parseLine[TestData3](Seq("1", "t 22", "333.0")) match {
      case Success(testData3) =>
        testData3 shouldBe TestData3(Some(1), Some("t 22"), Some(333.0))
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

  def errorHandler(err: String): Unit = {
    println(s"ERROR: $err")
    fail(err)
  }

  test("parseSource with comma") {
    def handler(testData: TestData1): Unit = {
      println(testData)
      testData.i match {
        case 123 => testData.s shouldBe "111111 111111111111 1111111"
        case 3 => testData.s shouldBe "a, b, c"
        case _ =>
      }
    }

    CsvReader.parseSource[TestData1](Source.fromResource("test-data1.csv"), handler, errorHandler)
  }

  test("parseSource with tab") {
    def handler(testData: TestData1): Unit = {
      println(testData)
      testData.i match {
        case 123 => testData.s shouldBe "111111 111111111111 1111111"
        case 3 => testData.s shouldBe "a, b, c"
        case _ =>
      }
    }

    CsvReader.parseSource[TestData1](Source.fromResource("test-data2.csv"), handler, errorHandler, '\t')
  }

  test("parseSource with tab and without quotas") {
    def handler(testData: TestData1): Unit = {
      println(testData)
      testData.i match {
        case 123 => testData.s shouldBe "111111 111111111111 1111111"
        case 3 => testData.s shouldBe "a, b, c"
        case _ =>
      }
    }

    CsvReader.parseSource[TestData1](Source.fromResource("test-data3.csv"), handler, errorHandler, '\t')
  }
}
