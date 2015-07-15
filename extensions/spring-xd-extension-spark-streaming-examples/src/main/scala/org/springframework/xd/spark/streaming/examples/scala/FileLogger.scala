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

import java.io.{BufferedWriter, File, FileWriter, IOException}
import java.util.Properties

import org.apache.spark.streaming.dstream.{DStream, ReceiverInputDStream}
import org.springframework.xd.spark.streaming.SparkConfig
import org.springframework.xd.spark.streaming.scala.Processor

/**
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
class FileLogger extends Processor[String, String] {

  var file: File = null

  def setPath(filePath: String) {
    file = new File(filePath)
    if (!file.exists) {
      try {
        file.createNewFile
      }
      catch {
        case ioe: IOException => {
          throw new RuntimeException(ioe)
        }
      }
    }
  }

  @SparkConfig def getSparkConfigProperties: Properties = {
    val props: Properties = new Properties
    props.setProperty("spark.master", "local[4]")
    return props
  }

  def process(input: ReceiverInputDStream[String]): DStream[String] = {
      input.foreachRDD(rdd => {
        rdd.foreachPartition(partition => {
          var fw: FileWriter = null
          var bw: BufferedWriter = null
          try {
            fw = new FileWriter(file.getAbsoluteFile)
            bw = new BufferedWriter(fw)
            while (partition.hasNext) {
              bw.append(partition.next.toString + System.lineSeparator)
            }
          }
          catch {
            case ioe: IOException => {
              throw new RuntimeException(ioe)
            }
          }
          finally {
            if (bw != null) {
              bw.close
            }
          }
        })
      })
    null
  }
}
