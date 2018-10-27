/*
 * Copyright (c) 2018. Yuriy Stul
 */

package com.stulsoft.csvreader

import java.lang.reflect.Constructor

import scala.io.Source
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

/**
  * Initializes a new instance of the CsvReader class.
  *
  * @param classTag the data type
  * @tparam T the data type
  */
class CsvReader[T] private()(implicit classTag: ClassTag[T]) {
  private var source: Source = _
  private var recordHandler: T => Unit = _
  private var errorHandler: String => Unit = _
  private var optionConstructor: Option[Constructor[_]] = _
  private var constructor: Constructor[_] = _
  private var delimiter: Char = CsvReader.DEFAULT_DELIMITER
  private var continueOnError = true
  private var hasHeaderLine = false

  /**
    * Specifies a delimiter. Default value is comma.
    *
    * @param delimiter the delimiter
    * @return CsvReader
    */
  def withDelimiter(delimiter: Char): CsvReader[T] = {
    this.delimiter = delimiter
    this
  }

  /**
    * Specifies either continue on error or stop. Default value is true.
    *
    * @param continueOnError either continue (true) on error or stop (false). Default value is true.
    * @return CsvReader
    */
  def withContinueOnError(continueOnError: Boolean): CsvReader[T] = {
    this.continueOnError = continueOnError
    this
  }

  /**
    * Specifies whether source has header line. Default value is false.
    *
    * @param hasHeaderLine whether source has header line.
    * @return CsvReader
    */
  def withHeaderLine(hasHeaderLine: Boolean): CsvReader[T] = {
    this.hasHeaderLine = hasHeaderLine
    this
  }

  /**
    * Parses source. Calls recordHeader for each record. Calls errorHandler for each error in parsing.
    */
  def parse(): Unit = {
    val splitExpression = delimiter + "(?=([^\"]*\"[^\"]*\")*[^\"]*$)"
    val iterator = source.getLines()
    var continue = true
    var counter: Int = 0
    while (continue && iterator.hasNext) {
      val line = iterator.next()
      counter += 1
      if (!(hasHeaderLine && counter == 1)) {
        parseLine(line
          .split(splitExpression)
          .map(_.trim)
          .map(field =>
            if (field.startsWith("\"") && field.endsWith("\""))
              field.substring(1, field.length - 1)
            else
              field
          )
        ) match {
          case Success(t: T) => recordHandler(t)
          case Failure(exception) =>
            continue = continueOnError
            errorHandler(s"Failed parse [$line]. Error: ${exception.getMessage}")
        }
      }
    }
  }

  /**
    * Parses one line.
    *
    * @param fields   collection of fields
    * @param classTag result data type
    * @return the result dat object
    */
  def parseLine(fields: Seq[String])(implicit classTag: ClassTag[T]): Try[T] = {
    Try {
      if (constructor.getParameterTypes.exists(pt => pt.getName == "scala.Option")) {
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
}

/**
  * @author Yuriy Stul
  */
object CsvReader {
  private val DEFAULT_DELIMITER = ','

  /**
    * Creates a new instance of the CsvReader.
    *
    * @param source        the source
    * @param recordHandler the record handler
    * @param errorHandler  the error handler
    * @param classTag      the result data type
    * @tparam T the result data type
    * @return the new instance of the CsvReader
    */
  def reader[T](source: Source,
                recordHandler: T => Unit,
                errorHandler: String => Unit)
               (implicit classTag: ClassTag[T]): CsvReader[T] = {
    val reader = new CsvReader[T]()
    reader.source = source
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
              && ctor.getParameterTypes.head.getName == "scala.collection.Seq")
            .head)
    }

    // constructor
    reader.constructor = if (reader.optionConstructor.isDefined)
      constructors.filter(ctor => ctor != reader.optionConstructor.get).head
    else
      constructors.head

    // validate constructors
    if (reader.constructor.getParameterTypes.exists(pt => pt.getName == "scala.Option")
      && reader.optionConstructor.isEmpty)
      throw new RuntimeException(s"Constructor for Option parameter(s), for ${classTag.runtimeClass.getName} class is not defined ")

    reader
  }
}
