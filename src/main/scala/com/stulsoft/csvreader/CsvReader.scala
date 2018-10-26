/*
 * Copyright (c) 2018. Yuriy Stul
 */

package com.stulsoft.csvreader

import java.lang.reflect.Constructor
import java.util.logging.{Level, LogManager, Logger}

import scala.io.Source
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

/**
  * @author Yuriy Stul
  */
object CsvReader {

  /**
    *
    * @param source       the source
    * @param handler      will be called for each line
    * @param errorHandler will be called for error line
    * @param delimiter    the delimter, default is comma
    * @param classTag     result type
    * @tparam T result type
    */
  def parseSource[T](source: Source, handler: T => Unit, errorHandler: String => Unit, delimiter: Char = ',')(implicit classTag: ClassTag[T]): Unit = {
    val splitExpression = delimiter + "(?=([^\"]*\"[^\"]*\")*[^\"]*$)"
    val iterator = source.getLines()
    while (iterator.hasNext) {
      val line = iterator.next()
      parseLine[T](line
        .split(splitExpression)
        .map(_.trim)
        .map(field =>
          if (field.startsWith("\"") && field.endsWith("\""))
            field.substring(1, field.length - 1)
          else
            field
        )
      ) match {
        case Success(t: T) => handler(t)
        case Failure(exception) =>
          errorHandler(s"Failed parse [$line]. Error: ${exception.getMessage}")
      }
    }
  }

  /**
    * Parses one line
    *
    * @param fields   collection of the fields
    * @param classTag the object type
    * @tparam T an object type
    * @return new instance of the T type
    * @see [[https://meta.plasm.us/posts/2015/11/08/type-classes-and-generic-derivation/ Type classes and generic derivation]]
    */
  def parseLine[T](fields: Seq[String])(implicit classTag: ClassTag[T]): Try[T] = {
    Try {
      val constructors = classTag.runtimeClass.getConstructors

      val optionConstructor: Option[Constructor[_]] = constructors.size match {
        case 1 => None
        case _ =>
          Some(
            constructors
              .filter(ctor => ctor.getParameterCount == 1
                && ctor.getParameterTypes.head.getName == "scala.collection.Seq") // Todo should be more smarty!!!!
              .head)
      }

      val constructor = if (optionConstructor.isDefined)
        constructors.filter(ctor => ctor != optionConstructor.get).head
      else
        constructors.head

      if (constructor.getParameterTypes.exists(pt => pt.getName == "scala.Option")
        && optionConstructor.isEmpty)
        throw new RuntimeException(s"Constructor for Option parameter(s), for ${classTag.runtimeClass.getName} class is not defined ")

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
