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

package org.springframework.xd.spark.streaming;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDDLike;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaDStreamLike;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Invokes the process method of a {@link Processor} and handles the output DStream if present.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
@SuppressWarnings({"unchecked", "rawtypes", "serial"})
public class SparkStreamingModuleExecutor implements Serializable {

	private static SparkMessageSender messageSender;

	private static final String HEADER_PREFIX = "spark.";

	// todo: allow this to be overridden for any Spark Streaming Processor
	private final MessageConverter converter = new DefaultSparkStreamingMessageConverter();

	@SuppressWarnings("rawtypes")
	public void execute(JavaDStream input, Processor processor, final SparkMessageSender sender) {
		JavaDStreamLike output = processor.process(input);
		if (output != null) {
			output.foreachRDD(new Function<JavaRDDLike, Void>() {

				@Override
				public Void call(final JavaRDDLike rdd) {
					rdd.foreachPartition(new VoidFunction<Iterator<?>>() {

						@Override
						public void call(Iterator<?> results) throws Exception {
							if (results.hasNext()) {
								if (messageSender == null) {
									messageSender = sender;
								}
								try {
									messageSender.start();
								}
								catch (NoSuchBeanDefinitionException e) {
									// ignore for the first time.
								}
								finally {
									if (sender != null && !sender.isRunning()) {
										messageSender.stop();
										messageSender = sender;
										messageSender.start();
									}
								}
								while (results.hasNext()) {
									messageSender.send(converter.toMessage(results.next(), populateHeaders(rdd)));
								}
							}
							sender.stop();
						}
					});
					return null;
				}
			});
		}
	}

	private MessageHeaders populateHeaders(JavaRDDLike rdd) {
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put(HEADER_PREFIX + "rdd.id", rdd.id());
		headers.put(HEADER_PREFIX + "rdd.name", rdd.name());
		SparkContext context = rdd.context();
		if (context != null) {
			headers.put(HEADER_PREFIX + "app.id", context.applicationId());
			headers.put(HEADER_PREFIX + "app.name", context.appName());
		}
		return new MessageHeaders(headers);
	}


	private static class DefaultSparkStreamingMessageConverter implements MessageConverter, Serializable {

		@Override
		public Object fromMessage(Message<?> message, Class<?> targetClass) {
			throw new UnsupportedOperationException("converter only used for creating Messages");
		}

		@Override
		public Message<?> toMessage(Object payload, MessageHeaders headers) {
			Class<?> clazz = payload.getClass();
			if (!(clazz.isPrimitive() || clazz.getName().startsWith("java."))) {
				payload = payload.toString();
			}
			return MessageBuilder.withPayload(payload).build();
		}
	}

}
