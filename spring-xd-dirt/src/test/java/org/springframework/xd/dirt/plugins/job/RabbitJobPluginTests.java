/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Rule;

import org.springframework.integration.codec.Codec;
import org.springframework.integration.codec.kryo.PojoCodec;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.integration.bus.rabbit.RabbitTestMessageBus;
import org.springframework.xd.test.rabbit.RabbitTestSupport;
import org.springframework.xd.tuple.serializer.kryo.TupleKryoRegistrar;


/**
 * @author Gary Russell
 * @author David Turanski
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

	@SuppressWarnings({"unchecked", "rawtypes"})
	protected Codec getCodec() {
		return new PojoCodec(new TupleKryoRegistrar());
	}

}
