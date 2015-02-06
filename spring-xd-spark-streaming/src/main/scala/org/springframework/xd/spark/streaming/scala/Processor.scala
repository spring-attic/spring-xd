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
package org.springframework.xd.spark.streaming.scala

import org.apache.spark.streaming.dstream.{DStream, ReceiverInputDStream}
import org.springframework.xd.spark.streaming.SparkStreamingSupport

/**
 * Trait for scala modules using the Spark Streaming API to process messages from the Message Bus.
 * @tparam I the type of the object received in the stream
 * @tparam O the type of the object returned after the computation
 *
 * @author Ilayaperumal Gopinathan
 * @since 1.1
 */
trait Processor[I, O] extends SparkStreamingSupport {

  /**
   * Processes the input DStream and optionally returns an output DStream.
   *
   * @param input the input DStream from the receiver
   * @return output DStream (optional, may be null)
   */
  def process(input: ReceiverInputDStream[I]): DStream[O]

}
