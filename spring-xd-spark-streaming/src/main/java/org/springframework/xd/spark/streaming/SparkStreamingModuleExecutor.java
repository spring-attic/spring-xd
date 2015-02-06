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

/**
 * The interface that streaming module executor will implement that runs the computation logic on the input DStream
 * and send the output via ${@link SparkMessageSender}.
 *
 * @author Ilayaperumal Gopinathan
 * @since 1.1
 */
public interface SparkStreamingModuleExecutor<I, P extends SparkStreamingSupport> {

	/**
	 * The execute method that runs the computation on input by invoking the underlying
	 * processor's process method and make the output available for
	 * ${@link org.springframework.xd.spark.streaming.SparkMessageSender} if needed.
	 *
	 * @param input the input received by the spark streaming receiver
	 * @param processor the underlying processor implementation (java or scala based)
	 * @param sender the message sender
	 */
	public void execute(I input, P processor, SparkMessageSender sender);
}
