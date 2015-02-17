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

package org.springframework.xd.spark.streaming.java;

import java.io.Serializable;
import java.util.Iterator;

import org.apache.spark.api.java.JavaRDDLike;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.api.java.JavaDStreamLike;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.xd.spark.streaming.SparkMessageSender;
import org.springframework.xd.spark.streaming.SparkStreamingModuleExecutor;

/**
 * Invokes the process method of a {@link org.springframework.xd.spark.streaming.java.Processor}
 * and handles the output DStream if present.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 * @since 1.1
 */
@SuppressWarnings({"unchecked", "rawtypes", "serial"})
public class ModuleExecutor implements SparkStreamingModuleExecutor<JavaReceiverInputDStream, Processor>, Serializable {

	private static SparkMessageSender messageSender;

	@SuppressWarnings("rawtypes")
	public void execute(JavaReceiverInputDStream input, Processor processor, final SparkMessageSender sender) {
		JavaDStreamLike output = processor.process(input);
		if (output != null) {
			output.foreachRDD(new Function<JavaRDDLike, Void>() {
				@Override
				public Void call(final JavaRDDLike rdd) {
					rdd.foreachPartition(new VoidFunction<Iterator<?>>() {
						@Override
						public void call(Iterator<?> results) throws Exception {
							if (messageSender == null) {
								messageSender = sender;
								messageSender.start();
							}
							while (results.hasNext()) {
								Object next = results.next();
								Message message = (next instanceof Message) ? (Message) next :
										MessageBuilder.withPayload(next).build();
								messageSender.send(message);
							}
						}
					});
					return null;
				}
			});
			if (messageSender != null) {
				messageSender.stop();
				messageSender = null;
			}
		}
	}

}
