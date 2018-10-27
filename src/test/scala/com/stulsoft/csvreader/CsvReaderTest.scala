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

  test("parseLine") {
    val i = 123
    val s = "some text"
    val d = 321.98
    CsvReader.reader[TestData1](Source.fromString(""),
      td1 => td1 shouldBe TestData1(i, s, d),
      err => println(err)
    )
      .parseLine(Seq(i.toString, s, d.toString)) match {
      case Success(testData1) =>
        testData1 shouldBe TestData1(i, s, d)
      case Failure(exception) =>
        fail(exception.getMessage)
    }
  }

  test("parseLine with empty option parameter") {
    val i = 123
    val d = 321.98
    CsvReader.reader[TestData2](Source.fromString(""),
      td1 => td1 shouldBe TestData2(i, None, d),
      err => println(err))
      .parseLine(Seq(i.toString, "", d.toString)) match {
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
    CsvReader.reader[TestData2](Source.fromString(""),
      td1 => td1 shouldBe TestData1(i, s, d),
      err => println(err)
    ).parseLine(Seq(i.toString, s, d.toString)) match {
      case Success(testData2) =>
        testData2 shouldBe TestData2(i, Some(s), d)
      case Failure(exception) =>
        fail(exception.getMessage)
    }
  }

  test("parseLine all empty") {
    CsvReader.reader[TestData3](Source.fromString(""),
      td1 => td1 shouldBe TestData3(None, None, None),
      err => println(err))
      .parseLine(Seq("", "", "")) match {
      case Success(testData3) =>
        testData3 shouldBe TestData3(None, None, None)
      case Failure(exception) =>
        fail(exception.getMessage)
    }
  }

  test("parseLine all defined") {
    CsvReader.reader[TestData3](Source.fromString(""),
      td1 => td1 shouldBe TestData3(Some(1), Some("t 22"), Some(333.0)),
      err => println(err))
      .parseLine(Seq("1", "t 22", "333.0")) match {
      case Success(testData3) =>
        testData3 shouldBe TestData3(Some(1), Some("t 22"), Some(333.0))
      case Failure(exception) =>
        fail(exception.getMessage)
    }
  }

  test("parseLine with error") {
    val i = 123
    val s = "some text"
    CsvReader.reader[TestData1](Source.fromString(""),
      _ => {},
      _ => {}
    )
      .parseLine(Seq(i.toString, s)) match {
      case Success(_) =>
        fail("Exception should be thrown")
      case Failure(_) =>
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

    CsvReader.reader[TestData1](Source.fromResource("test-data1.csv"), handler, errorHandler)
      .parse()
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

    CsvReader.reader[TestData1](Source.fromResource("test-data2.csv"), handler, errorHandler)
      .withDelimiter('\t')
      .parse()
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

    CsvReader.reader[TestData1](Source.fromResource("test-data3.csv"), handler, errorHandler)
      .withDelimiter('\t')
      .parse()
  }

  test("initialize CsvReader") {
    def recordHandler(record: TestData1): Unit = {

    }

    def errorHandler(err: String): Unit = {

    }

    CsvReader.reader[TestData1](Source.fromResource("test-data1.csv"),
      recordHandler,
      errorHandler
    ) should not be null
  }
}
