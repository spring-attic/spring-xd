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

package org.springframework.xd.dirt.plugins.job;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;

import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.integration.x.bus.RabbitTestMessageBus;
import org.springframework.integration.x.bus.serializer.AbstractCodec;
import org.springframework.integration.x.bus.serializer.CompositeCodec;
import org.springframework.integration.x.bus.serializer.MultiTypeCodec;
import org.springframework.integration.x.bus.serializer.kryo.PojoCodec;
import org.springframework.integration.x.bus.serializer.kryo.TupleCodec;
import org.springframework.xd.test.rabbit.RabbitTestSupport;
import org.springframework.xd.tuple.Tuple;


/**
 * 
 * @author Gary Russell
 */
public class RabbitJobPluginTests extends JobPluginTests {

	@Rule
	public RabbitTestSupport rabbitAvailableRule = new RabbitTestSupport();

	@Override
	protected MessageBus getMessageBus() {
		if (testMessageBus == null) {
			testMessageBus = new RabbitTestMessageBus(rabbitAvailableRule.getResource(), getCodec());
		}
		return testMessageBus;
	}

	@Override
	protected void checkBusBound(MessageBus bus) {
		if (bus instanceof RabbitTestMessageBus) {
			MessageBus msgBus = ((RabbitTestMessageBus) bus).getCoreMessageBus();
			assertEquals(4, TestUtils.getPropertyValue(msgBus, "bindings", Collection.class).size());
		}
	}

	@Override
	protected void checkBusUnbound(MessageBus bus) {
		if (bus instanceof RabbitTestMessageBus) {
			MessageBus msgBus = ((RabbitTestMessageBus) bus).getCoreMessageBus();
			assertEquals(0, TestUtils.getPropertyValue(msgBus, "bindings", Collection.class).size());
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected MultiTypeCodec<Object> getCodec() {
		Map<Class<?>, AbstractCodec<?>> codecs = new HashMap<Class<?>, AbstractCodec<?>>();
		codecs.put(Tuple.class, new TupleCodec());
		return new CompositeCodec(codecs, new PojoCodec());
	}

}
