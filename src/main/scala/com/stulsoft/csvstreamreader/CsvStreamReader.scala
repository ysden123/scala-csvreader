/*
 * Copyright (c) 2018. Yuriy Stul
 */

package com.stulsoft.csvstreamreader

import java.lang.reflect.Constructor

import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink}
import akka.{Done, NotUsed}
import com.stulsoft.Commons
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

/**
  * @author Yuriy Stul
  */
class CsvStreamReader[T] private()(implicit classTag: ClassTag[T]) extends LazyLogging {
  private var source: akka.stream.scaladsl.Source[String, NotUsed] = _
  private var recordHandler: T => Unit = _
  private var errorHandler: String => Unit = _
  private lazy val flow: Flow[String, Try[T], NotUsed] = Flow[String].map(parseLine)
  private lazy val sink: Sink[Try[T], Future[Done]] = Sink.foreach {
    case Success(data) => recordHandler(data)
    case Failure(exception) =>
      val msg = s"Error: ${exception.getMessage}"
      logger.error(msg)
      errorHandler(msg)
  }
  private var optionConstructor: Option[Constructor[_]] = _
  private var constructor: Constructor[_] = _
  private var delimiter: Char = Commons.DEFAULT_DELIMITER
  private var continueOnError = true
  private var hasHeaderLine = false
  private var customTransformer: (Seq[String], T => Unit, String => Unit) => T = _
  private lazy val splitExpression = delimiter + Commons.DELIMITER_REG_EXPRESSION
  private var continue = true

  /**
    * Specifies a delimiter. Default value is comma.
    *
    * @param delimiter the delimiter
    * @return CsvReader
    */
  def withDelimiter(delimiter: Char): CsvStreamReader[T] = {
    this.delimiter = delimiter
    this
  }

  /**
    * Specifies either continue on error or stop. Default value is true.
    *
    * @param continueOnError either continue (true) on error or stop (false). Default value is true.
    * @return CsvReader
    */
  def withContinueOnError(continueOnError: Boolean): CsvStreamReader[T] = {
    this.continueOnError = continueOnError
    this
  }

  /**
    * Specifies whether source has header line. Default value is false.
    *
    * @param hasHeaderLine whether source has header line.
    * @return CsvReader
    */
  def withHeaderLine(hasHeaderLine: Boolean): CsvStreamReader[T] = {
    this.hasHeaderLine = hasHeaderLine
    this
  }

  /**
    * Specifies a customer transformer.
    *
    * First parameter is collection of the strings (CSV record line).
    *
    * Second parameter is record handler, should be called after creating T object.
    *
    * Third parameter is error handler, should be called in case any error was occurred during creating T object.
    *
    * @param customTransformer the customer transformer which converts line into T
    * @return CsvReader
    */
  def withCustomTransformer(customTransformer: (Seq[String], T => Unit, String => Unit) => T): CsvStreamReader[T] = {
    this.customTransformer = customTransformer
    this
  }

  def parseLine(line: String): Try[T] = {
    convertFieldsToT(line.split(splitExpression)
      .map(_.trim)
      .map(field =>
        if (field.startsWith("\"") && field.endsWith("\""))
          field.substring(1, field.length - 1)
        else
          field
      )) match {
      case Success(data) => Success(data)
      case Failure(exception) =>
        val msg = s"Failed parse [$line]. Error: ${exception.getMessage}"
        logger.error(msg)
        errorHandler(msg)
        continue = continueOnError
        Failure(exception)
    }
  }

  /**
    * Converts fields to T type.
    *
    * @param fields   collection of fields
    * @param classTag result data type
    * @return the result data object
    */
  def convertFieldsToT(fields: Seq[String])(implicit classTag: ClassTag[T]): Try[T] = {
    Try {
      if (customTransformer != null) {
        customTransformer(fields, recordHandler, errorHandler)
      } else if (constructor.getParameterTypes.exists(pt => pt.getName == "scala.Option")) {
        optionConstructor.get.newInstance(fields).asInstanceOf[T]
      } else {
        val fieldsWithTypes = fields.zip(constructor.getParameterTypes)
        val parameters = fieldsWithTypes.map {
          case (field, clazz) => clazz.getName match {
            case "int" => field.toInt.asInstanceOf[Object]
            case "double" => field.toDouble.asInstanceOf[Object]
            case _ =>
              clazz
                .getConstructor(field.getClass)
                .newInstance(field)
                .asInstanceOf[Object]
          }
        }
        constructor.newInstance(parameters: _*).asInstanceOf[T]
      }
    }
  }

  /** Executes flow
    *
    * @param materializer the materializer
    * @return Future[Done]
    */
  def run()(implicit materializer: Materializer): Future[Done] = {
    source.drop(if (hasHeaderLine) 1 else 0)
      .takeWhile(_ => continue)
      .via(flow)
      .runWith(sink)
  }
}

object CsvStreamReader extends LazyLogging {
  /**
    * Creates a new instance of the CsvStreamReader.
    *
    * @param source        the source
    * @param recordHandler the record handler
    * @param errorHandler  the error handler
    * @param classTag      the result data type
    * @tparam T the result data type
    * @return the new instance of the CsvReader
    */
  def reader[T](source: scala.io.Source,
                recordHandler: T => Unit,
                errorHandler: String => Unit)
               (implicit classTag: ClassTag[T]): CsvStreamReader[T] = {
    val reader = new CsvStreamReader[T]()
    reader.source = akka.stream.scaladsl.Source.fromIterator(source.getLines)
    reader.recordHandler = recordHandler
    reader.errorHandler = errorHandler

    // define constructors
    val constructors = classTag.runtimeClass.getConstructors
    // constructor for case Option params are using
    reader.optionConstructor = constructors.size match {
      case 1 => None
      case _ =>
        Some(
          constructors
            .filter(ctor => ctor.getParameterCount == 1
              && ctor.getParameterTypes.head.getName == "scala.collection.immutable.Seq")
            .head)
    }

    // constructor
    reader.constructor = if (reader.optionConstructor.isDefined)
      constructors.filter(ctor => ctor != reader.optionConstructor.get).head
    else
      constructors.head

    // validate constructors
    if (reader.constructor.getParameterTypes.exists(pt => pt.getName == "scala.Option")
      && reader.optionConstructor.isEmpty) {
      val msg = s"Constructor for Option parameter(s), for ${classTag.runtimeClass.getName} class is not defined "
      logger.error(s"Constructor for Option parameter(s), for ${classTag.runtimeClass.getName} class is not defined ")
      throw new RuntimeException(msg)
    }

    reader
  }
}
