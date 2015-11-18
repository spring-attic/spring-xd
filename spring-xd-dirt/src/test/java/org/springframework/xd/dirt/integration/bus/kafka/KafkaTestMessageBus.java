/*
 * Copyright 2014-2015 the original author or authors.
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

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.codec.Codec;
import org.springframework.integration.codec.kryo.PojoCodec;
import org.springframework.integration.kafka.support.ZookeeperConnect;
import org.springframework.xd.dirt.integration.bus.AbstractTestMessageBus;
import org.springframework.xd.dirt.integration.kafka.KafkaMessageBus;
import org.springframework.xd.dirt.integration.kafka.TestKafkaCluster;
import org.springframework.xd.test.kafka.KafkaTestSupport;
import org.springframework.xd.tuple.serializer.kryo.TupleKryoRegistrar;


/**
 * Test support class for {@link KafkaMessageBus}.
 * Creates a bus that uses a test {@link TestKafkaCluster kafka cluster}.
 * @author Eric Bottard
 * @author Marius Bogoevici
 * @author David Turanski
 * @author Gary Russell
 */
public class KafkaTestMessageBus extends AbstractTestMessageBus<KafkaMessageBus> {

	public KafkaTestMessageBus(KafkaTestSupport kafkaTestSupport) {
		this(kafkaTestSupport, getCodec(), KafkaMessageBus.Mode.embeddedHeaders);
	}


	public KafkaTestMessageBus(KafkaTestSupport kafkaTestSupport, Codec codec,
			KafkaMessageBus.Mode mode, String... headers) {

		try {
			ZookeeperConnect zookeeperConnect = new ZookeeperConnect();
			zookeeperConnect.setZkConnect(kafkaTestSupport.getZkConnectString());
			KafkaMessageBus messageBus = new KafkaMessageBus(zookeeperConnect,
					kafkaTestSupport.getBrokerAddress(),
					kafkaTestSupport.getZkConnectString(), codec, headers);
			messageBus.setDefaultBatchingEnabled(false);
			messageBus.setMode(mode);
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

	public KafkaTestMessageBus(KafkaTestSupport kafkaTestSupport, Codec codec,
			KafkaMessageBus.OffsetManagement offsetManagement) {

		try {
			ZookeeperConnect zookeeperConnect = new ZookeeperConnect();
			zookeeperConnect.setZkConnect(kafkaTestSupport.getZkConnectString());
			KafkaMessageBus messageBus = new KafkaMessageBus(zookeeperConnect,
					kafkaTestSupport.getBrokerAddress(),
					kafkaTestSupport.getZkConnectString(), codec);
			messageBus.setOffsetManagement(offsetManagement);
			messageBus.setDefaultBatchingEnabled(false);
			messageBus.setMode(KafkaMessageBus.Mode.embeddedHeaders);
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
	private static Codec getCodec() {
		return new PojoCodec(new TupleKryoRegistrar());
	}

}
