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

import org.springframework.messaging.Message;

/**
 * Abstract class that defines abstract methods to support sending the computed messages out of Spark cluster
 * to XD MessageBus etc.
 *
 * @author Ilayaperumal Gopinathan
 * @since 1.1
 */
public abstract class SparkMessageSender implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Start the message sender
	 */
	public abstract void start();

	/**
	 * Stop the message sender
	 */
	public abstract void stop();

	/**
	 * Check if the message sender is running
	 * @return boolean true if the sender is running
	 */
	public abstract boolean isRunning();

	/**
	 * Send a message out of Spark cluster.
	 *
	 * @param message the message to send
	 */
	@SuppressWarnings("rawtypes")
	public abstract void send(Message message);

}
