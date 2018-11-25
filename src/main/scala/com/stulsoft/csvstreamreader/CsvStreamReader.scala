/*
 * Copyright (c) 2018. Yuriy Stul
 */

package com.stulsoft.csvstreamreader

import akka.{Done, NotUsed}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
  * @author Yuriy Stul
  */
class CsvStreamReader[T] private()(implicit classTag: ClassTag[T]) extends LazyLogging {
  private var source: Source[String, NotUsed] = _
  private var flow: Flow[String, T, NotUsed] = _
  private var sinc: Sink[T, Future[Done]] = _
}
