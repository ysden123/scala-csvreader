/*
 * Copyright (c) 2018. Yuriy Stul
 */

package com.stulsoft.csvreader

/**
  * @author Yuriy Stul
  */
case class TestData2(i: Int, s: Option[String], d: Double) {
  def this(params: Seq[String]) {
    this(params(0).toInt,
      if (params(1).isEmpty)
        None
      else
        Some(params(1)),
      params(2).toDouble
    )
  }
}
