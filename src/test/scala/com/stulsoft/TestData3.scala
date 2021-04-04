/*
 * Copyright (c) 2018. Yuriy Stul
 */

package com.stulsoft

/**
  * @author Yuriy Stul
  */
case class TestData3(i: Option[Int], s: Option[String], d: Option[Double]) {
  def this(params: Seq[String]) = {
    this(
      if (params(0).isEmpty) None else Some(params(0).toInt),
      if (params(1).isEmpty) None else Some(params(1)),
      if (params(2).isEmpty) None else Some(params(2).toDouble)
    )
  }
}

object TT extends App {
  //  println(TestData3("1", "text","32"))
  //  println(TestData3("1", "text",""))
  //  println(TestData3("1", "","32"))
  //  println(TestData3("", "text","32"))
  val params = Seq("", "text", "32")
  println(new TestData3(params))
}