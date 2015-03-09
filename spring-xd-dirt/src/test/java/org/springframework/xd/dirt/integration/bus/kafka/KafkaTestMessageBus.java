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

package org.springframework.xd.dirt.integration.bus.kafka;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.kafka.support.ZookeeperConnect;
import org.springframework.xd.dirt.integration.bus.AbstractTestMessageBus;
import org.springframework.xd.dirt.integration.bus.serializer.AbstractCodec;
import org.springframework.xd.dirt.integration.bus.serializer.CompositeCodec;
import org.springframework.xd.dirt.integration.bus.serializer.MultiTypeCodec;
import org.springframework.xd.dirt.integration.bus.serializer.kryo.PojoCodec;
import org.springframework.xd.dirt.integration.bus.serializer.kryo.TupleCodec;
import org.springframework.xd.dirt.integration.kafka.KafkaMessageBus;
import org.springframework.xd.dirt.integration.kafka.TestKafkaCluster;
import org.springframework.xd.test.kafka.KafkaTestSupport;
import org.springframework.xd.tuple.Tuple;


/**
 * Test support class for {@link KafkaMessageBus}.
 * Creates a bus that uses a test {@link TestKafkaCluster kafka cluster}.
 *
 * @author Eric Bottard
 * @author Marius Bogoevici
 */
public class KafkaTestMessageBus extends AbstractTestMessageBus<KafkaMessageBus> {

	public KafkaTestMessageBus(KafkaTestSupport kafkaTestSupport) {
		this(kafkaTestSupport, getCodec());
	}


	public KafkaTestMessageBus(KafkaTestSupport kafkaTestSupport, MultiTypeCodec<Object> codec) {

		try {
			ZookeeperConnect zookeeperConnect = new ZookeeperConnect();
			zookeeperConnect.setZkConnect(kafkaTestSupport.getZkConnectString());
			KafkaMessageBus messageBus = new KafkaMessageBus(zookeeperConnect,
					kafkaTestSupport.getBrokerAddress(),
					kafkaTestSupport.getZkConnectString(), codec);
			messageBus.setDefaultBatchingEnabled(false);
			messageBus.afterPropertiesSet();
			GenericApplicationContext context = new GenericApplicationContext();
			context.refresh();
			messageBus.setApplicationContext(context);
			this.setMessageBus(messageBus);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void cleanup() {
		// do nothing - the rule will take care of that
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static MultiTypeCodec<Object> getCodec() {
		Map<Class<?>, AbstractCodec<?>> codecs = new HashMap<Class<?>, AbstractCodec<?>>();
		codecs.put(Tuple.class, new TupleCodec());
		return new CompositeCodec(codecs, new PojoCodec());
	}

}
