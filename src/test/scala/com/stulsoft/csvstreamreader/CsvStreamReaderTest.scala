/*
 * Copyright (c) 2018. Yuriy Stul
 */

package com.stulsoft.csvstreamreader

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.stulsoft.TestData1
import org.scalatest.{FunSuite, Matchers}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source

/**
  * @author Yuriy Stul
  */
class CsvStreamReaderTest extends FunSuite with Matchers {

  test("create instance of CsvStreamReader") {
    def recordHandler(data: TestData1): Unit = {}

    def errorHandler(errorMessage: String): Unit = {}

    val source = Source.fromString("")

    val reader = CsvStreamReader.reader(source, recordHandler, errorHandler)
    reader should not be null
  }

  test("read correct CSV file") {
    implicit val actorSystem: ActorSystem = ActorSystem("CsvStreamReaderTest")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    val counter = new AtomicInteger

    def recordHandler(data: TestData1): Unit = counter.getAndIncrement()

    def errorHandler(errorMessage: String): Unit = {}

    val source = Source.fromResource("test-data1.csv")

    val reader = CsvStreamReader.reader(source, recordHandler, errorHandler)

    Await.ready(reader.run(), Duration.Inf)
    source.close()
    counter.get() shouldBe 3
    actorSystem.terminate()
  }

  test("read incorrect CSV file") {
    implicit val actorSystem: ActorSystem = ActorSystem("CsvStreamReaderTest")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    val counter = new AtomicInteger

    def recordHandler(data: TestData1): Unit = counter.getAndIncrement()

    def errorHandler(errorMessage: String): Unit = {}

    val source = Source.fromResource("test-data4.csv")

    val reader = CsvStreamReader.reader(source, recordHandler, errorHandler)

    Await.ready(reader.run(), Duration.Inf)
    source.close()
    counter.get() shouldBe 2
    actorSystem.terminate()
  }

  test("read incorrect CSV file without continue on error") {
    implicit val actorSystem: ActorSystem = ActorSystem("CsvStreamReaderTest")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    val counter = new AtomicInteger

    def recordHandler(data: TestData1): Unit = counter.getAndIncrement()

    def errorHandler(errorMessage: String): Unit = {}

    val source = Source.fromResource("test-data4.csv")

    val reader = CsvStreamReader.reader[TestData1](source, recordHandler, errorHandler)
      .withContinueOnError(false)

    Await.ready(reader.run(), Duration.Inf)
    source.close()
    counter.get() shouldBe 1
    actorSystem.terminate()
  }

  test("customTransformer") {
    implicit val actorSystem: ActorSystem = ActorSystem("CsvStreamReaderTest")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    val counter = new AtomicInteger

    def transformer(line: Seq[String], recordHandler: TestData1 => Unit, errorHandler: String => Unit): TestData1 = {
      try {
        val data = TestData1(333, "test", 77.12)
        data
      } catch {
        case e: Exception =>
          errorHandler(e.getMessage)
          throw e
      }
    }

    val source = Source.fromString("fgfghfhgfhg")
    val reader = CsvStreamReader
      .reader[TestData1](source,
      l => counter.getAndIncrement(),
      e => println(s"Error: $e"))
      .withCustomTransformer(transformer)

    Await.ready(reader.run(), Duration.Inf)
    source.close()
    counter.get() shouldBe 1
    actorSystem.terminate()
  }

  test("customTransformer with error") {
    implicit val actorSystem: ActorSystem = ActorSystem("CsvStreamReaderTest")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    val counter = new AtomicInteger
    def transformer(line: Seq[String], recordHandler: TestData1 => Unit, errorHandler: String => Unit): TestData1 = {
      try {
        throw new RuntimeException("Test exception")
      } catch {
        case e: Exception =>
          errorHandler(e.getMessage)
          throw e
      }
    }

    val source = Source.fromString("fgfghfhgfhg")
    val reader = CsvStreamReader
      .reader[TestData1](source,
      l => counter.getAndIncrement(),
      e => println(s"Error: $e"))
      .withCustomTransformer(transformer)

    Await.ready(reader.run(), Duration.Inf)
    source.close()
    counter.get() shouldBe 0
    actorSystem.terminate()
  }

  test("with header line") {
    implicit val actorSystem: ActorSystem = ActorSystem("CsvStreamReaderTest")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    val counter = new AtomicInteger

    val source = Source.fromResource("test-data5.csv")
    val reader = CsvStreamReader.reader[TestData1](source,
      _ => counter.getAndIncrement(),
      e => println(s"Error: $e"))
      .withHeaderLine(true)

    Await.ready(reader.run(), Duration.Inf)
    source.close()
    counter.get() shouldBe 3
    actorSystem.terminate()
  }

  test("toList"){
    implicit val actorSystem: ActorSystem = ActorSystem("CsvStreamReaderTest")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    val list = ListBuffer.empty[TestData1]

    val source = Source.fromResource("test-data5.csv")
    val reader = CsvStreamReader.reader[TestData1](source,
      testData1 => list += testData1,
      e => println(s"Error: $e"))
      .withHeaderLine(true)

    Await.ready(reader.run(), Duration.Inf)
    source.close()
    list.length shouldBe 3
    actorSystem.terminate()

  }
}
