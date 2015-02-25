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
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.integration.support.MessageBuilder
import org.springframework.messaging.Message
import org.springframework.xd.spark.streaming.{SparkMessageSender, SparkStreamingModuleExecutor}

/**
 * Invokes the process method of a {@link org.springframework.xd.spark.streaming.scala.Processor}
 * and handles the output DStream if present.
 *
 * @author Ilayaperumal Gopinathan
 * @since 1.1
 */
class ModuleExecutor extends SparkStreamingModuleExecutor[ReceiverInputDStream[Any],
  org.springframework.xd.spark.streaming.scala.Processor[Any, Any]] with Serializable {
  private var messageSender: SparkMessageSender = null

  def execute(input: ReceiverInputDStream[Any],
              processor: org.springframework.xd.spark.streaming.scala.Processor[Any, Any],
              sender: SparkMessageSender) {
    val output: DStream[Any] = processor.process(input)
    if (output != null) {
      output.foreachRDD(rdd => {
        rdd.foreachPartition(partition => {
          if (messageSender == null) {
            messageSender = sender
            messageSender.start()
          }
          while (partition.hasNext) {
            val message = partition.next()
            if (message.isInstanceOf[Message[_]]) {
              messageSender.send(message.asInstanceOf[Message[_]])
            }
            else {
              messageSender.send(MessageBuilder.withPayload(message).build())
            }
          }
        })
      })
      if (messageSender != null) {
        messageSender.stop
        messageSender = null
      }
    }
  }
}
