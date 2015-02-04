/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.xd.spark.streaming.examples.scala

import java.util.Properties

import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming.dstream.{DStream, ReceiverInputDStream}
import org.springframework.xd.spark.streaming.SparkConfig
import org.springframework.xd.spark.streaming.scala.Processor

/**
 * @author Ilayaperumal Gopinathan
 */
class WordCount extends Processor[String, (String, Int)] {

  def process(input: ReceiverInputDStream[String]): DStream[(String, Int)] = {
      val words = input.flatMap(_.split(" "))
      val pairs = words.map(word => (word, 1))
      val wordCounts = pairs.reduceByKey(_ + _)
      wordCounts
  }

  @SparkConfig
  def properties : Properties = {
    val props = new Properties()
    props.setProperty("spark.master", "local[4]")
    props
  }

}
