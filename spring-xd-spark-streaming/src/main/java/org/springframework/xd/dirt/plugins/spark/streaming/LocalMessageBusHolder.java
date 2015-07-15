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

package org.springframework.xd.dirt.plugins.spark.streaming;

import java.io.Serializable;

import org.springframework.xd.dirt.integration.bus.MessageBus;

/**
 * This class holds the static reference to the message bus (especially local messagebus) and makes it available at
 * Spark executors in local mode. This makes it possible to use the same message bus object used by the container.
 *
 * @author Mark Fisher
 * @since 1.1
 */
@SuppressWarnings("serial")
class LocalMessageBusHolder implements Serializable {

	private static MessageBus BUS;

	public static void set(MessageBus messageBus) {
		BUS = messageBus;
	}

	public MessageBus get() {
		return BUS;
	}
}
