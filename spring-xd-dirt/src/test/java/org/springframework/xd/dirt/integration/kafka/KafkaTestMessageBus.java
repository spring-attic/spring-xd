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

package org.springframework.xd.dirt.integration.kafka;

import java.io.IOException;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.xd.dirt.integration.bus.AbstractTestMessageBus;
import org.springframework.xd.dirt.integration.bus.serializer.MultiTypeCodec;


/**
 * Test support class for {@link KafkaMessageBus}.
 * Creates a bus that uses a test {@link TestKafkaCluster kafka cluster}.
 *
 * @author Eric Bottard
 */
public class KafkaTestMessageBus extends AbstractTestMessageBus<KafkaMessageBus> {


	private TestKafkaCluster kafkaCluster;

	public KafkaTestMessageBus(MultiTypeCodec<Object> codec) {
		kafkaCluster = new TestKafkaCluster();

		KafkaMessageBus messageBus = new KafkaMessageBus(kafkaCluster.getKafkaBrokerString(),
				kafkaCluster.getZkConnectString(), codec);
		GenericApplicationContext context = new GenericApplicationContext();
		context.refresh();
		messageBus.setApplicationContext(context);
		this.setMessageBus(messageBus);
	}

	@Override
	public void cleanup() {
		try {
			kafkaCluster.stop();
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
