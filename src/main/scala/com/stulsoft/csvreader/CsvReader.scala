/*
 * Copyright (c) 2018. Yuriy Stul
 */

package com.stulsoft.csvreader

import scala.io.Source
import scala.reflect.ClassTag

/**
  * @author Yuriy Stul
  */
object CsvReader {
  def parseSource[T](source: Source)(implicit ct: ClassTag[T]): Unit = {
    //    val t =tag.unapply(Seq("11", "22"))
    //    println(t)
    val ctor = ct.runtimeClass.getConstructors.head
    ctor.getParameters.foreach(println)
    ctor.getParameters.foreach(p =>
      println(s"name: ${p.getName}, class name: ${p.getType.getSimpleName}")

    )
    //    val paramsWithTypes = paramsArray.zip(ctor.getParameterTypes)

  }

  def parseLine[T](fields: Seq[String])(implicit classTag: ClassTag[T]): T = {
    val constructor = classTag.runtimeClass.getConstructors.head
    val fieldsWithTypes = fields.zip(constructor.getParameterTypes)
    val parameters = fieldsWithTypes.map {
      case (field, clazz) => clazz.getName match {
        case "int" => field.toInt.asInstanceOf[Object]
        case "double" => field.toDouble.asInstanceOf[Object]
        case _ =>
          val parameterCponstructor = clazz.getConstructor(field.getClass)
          parameterCponstructor.newInstance(field).asInstanceOf[Object]
      }
    }
    constructor.newInstance(parameters: _*).asInstanceOf[T]
  }
}
