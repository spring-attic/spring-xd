/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.module.spark;

import java.io.Serializable;
import java.util.Iterator;

import org.apache.spark.api.java.JavaRDDLike;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaDStreamLike;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Invokes the process method of a {@link SparkModule} and handles the output DStream if present.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
@SuppressWarnings("unchecked")
class SparkModuleExecutor implements Serializable {

	public void execute(JavaDStream input, SparkModule module, final SparkMessageSender sender) {
		JavaDStreamLike output = module.process(input);
		if (output != null) {
			output.foreachRDD(new Function<JavaRDDLike, Void>() {

				@Override
				public Void call(JavaRDDLike rdd) {
					rdd.foreachPartition(new VoidFunction<Iterator<?>>() {

						@Override
						public void call(Iterator<?> results) throws Exception {
							if (results.hasNext()) {
								sender.start();
								while (results.hasNext()) {
									sender.send(MessageBuilder.withPayload("" + results.next()).build());
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

}
