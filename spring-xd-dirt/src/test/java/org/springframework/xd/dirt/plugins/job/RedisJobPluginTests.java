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

import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.integration.x.bus.serializer.AbstractCodec;
import org.springframework.integration.x.bus.serializer.CompositeCodec;
import org.springframework.integration.x.bus.serializer.MultiTypeCodec;
import org.springframework.integration.x.bus.serializer.kryo.PojoCodec;
import org.springframework.integration.x.bus.serializer.kryo.TupleCodec;
import org.springframework.integration.x.redis.RedisMessageBus;
import org.springframework.xd.test.redis.RedisTestSupport;
import org.springframework.xd.tuple.Tuple;


/**
 * 
 * @author Gary Russell
 */
public class RedisJobPluginTests extends JobPluginTests {

	@Rule
	public RedisTestSupport redisAvailable = new RedisTestSupport();

	@Override
	protected MessageBus getMessageBus() {
		RedisMessageBus redisMessageBus = new RedisMessageBus(redisAvailable.getResource(), getCodec());
		redisMessageBus.setIntegrationEvaluationContext(new StandardEvaluationContext());
		return redisMessageBus;
	}

	@Override
	protected void checkBusBound(MessageBus bus) {
		assertEquals(4, TestUtils.getPropertyValue(bus, "bindings", Collection.class).size());
	}

	@Override
	protected void checkBusUnbound(MessageBus bus) {
		assertEquals(0, TestUtils.getPropertyValue(bus, "bindings", Collection.class).size());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected MultiTypeCodec<Object> getCodec() {
		Map<Class<?>, AbstractCodec<?>> codecs = new HashMap<Class<?>, AbstractCodec<?>>();
		codecs.put(Tuple.class, new TupleCodec());
		return new CompositeCodec(codecs, new PojoCodec());
	}

}
