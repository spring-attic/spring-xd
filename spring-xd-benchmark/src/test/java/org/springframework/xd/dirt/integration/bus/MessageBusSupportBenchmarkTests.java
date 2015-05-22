/*
 * Copyright 2015 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.integration.bus;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.StopWatch;
import org.springframework.xd.dirt.integration.bus.serializer.AbstractCodec;
import org.springframework.xd.dirt.integration.bus.serializer.CompositeCodec;
import org.springframework.xd.dirt.integration.bus.serializer.kryo.PojoCodec;
import org.springframework.xd.tuple.serializer.kryo.TupleCodec;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * Performance benchmark tests for {@link MessageBusSupport}
 * @author David Turanski
 */
public class MessageBusSupportBenchmarkTests {

	private static MessageBusSupport messageBusSupport;

	/*
Baseline (before MessageValues):

StopWatch '': running time (millis) = 119480
-----------------------------------------
ms     %     Task name
-----------------------------------------
119480  100%  simple tuple codec

---

After MessageValues and caching payload types:

StopWatch '': running time (millis) = 99382
-----------------------------------------
ms     %     Task name
-----------------------------------------
99382  100%  simple tuple codec
*/

	@BeforeClass
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void initMessageBus() {
		TestMessageBus messageBus = new TestMessageBus();
		Map<Class<?>, AbstractCodec<?>> codecs = new HashMap<>();
		codecs.put(Tuple.class, new TupleCodec());
		messageBus.setCodec(new CompositeCodec(codecs, new PojoCodec()));
		messageBusSupport = messageBus;
	}

	@Test
	public void run() {
		StopWatch watch = new StopWatch("MessageBusSupport");
		watch.start("simple tuple codec");
		runBenchmark(TupleBuilder.tuple().of("foo", "bar", "val", 1234));
		watch.stop();
		watch.start("string payload");
		runBenchmark(StringUtils.leftPad("hello", 1000, "*"));
		watch.stop();
		System.out.println(watch.prettyPrint());
	}


	@SuppressWarnings({"unchecked", "rawtypes"})
	private void runBenchmark(Object payload) {
		Message<?> message = new GenericMessage(payload);
		int ITERATIONS = 1000000;
		for (int i = 0; i < ITERATIONS; i++) {
			MessageValues msg = messageBusSupport.serializePayloadIfNecessary(message
			);
			messageBusSupport.deserializePayloadIfNecessary(msg.toMessage());
			if (i > 0 && i % 100000 == 0) {
				System.out.println("completed " + i + " iterations.");
			}
		}
	}


	public static class TestMessageBus extends MessageBusSupport {

		@Override
		public void bindConsumer(String name, MessageChannel channel, Properties properties) {
		}

		@Override
		public void bindPubSubConsumer(String name, MessageChannel moduleInputChannel,
				Properties properties) {
		}

		@Override
		public void bindPubSubProducer(String name, MessageChannel moduleOutputChannel,
				Properties properties) {
		}

		@Override
		public void bindProducer(String name, MessageChannel channel, Properties properties) {
		}

		@Override
		public void bindRequestor(String name, MessageChannel requests, MessageChannel replies,
				Properties properties) {
		}

		@Override
		public void bindReplier(String name, MessageChannel requests, MessageChannel replies,
				Properties properties) {
		}
	}
}
