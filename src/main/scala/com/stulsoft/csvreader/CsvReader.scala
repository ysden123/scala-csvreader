/*
 * Copyright (c) 2018. Yuriy Stul
 */

package com.stulsoft.csvreader

import java.util.logging.{Level, LogManager, Logger}

import scala.io.Source
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

/**
  * @author Yuriy Stul
  */
object CsvReader {

  /**
    * Parses a Source
    *
    * @param source  the source
    * @param handler will be called for each line
    * @tparam T specifies the result object type
    */
  def parseSource[T](source: Source, handler: T => Unit, errorHandler: String => Unit)(implicit classTag: ClassTag[T]): Unit = {
    val iterator = source.getLines()
    while (iterator.hasNext) {
      val line = iterator.next()
      parseLine[T](line.split(',').map(_.trim)) match {
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
      val constructor = classTag.runtimeClass.getConstructors.head
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
