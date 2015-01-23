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


package org.springframework.integration.x.kafka;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;

import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.integration.kafka.core.DefaultConnectionFactory;
import org.springframework.integration.kafka.core.ZookeeperConfiguration;
import org.springframework.integration.kafka.support.ZookeeperConnect;
import org.springframework.xd.test.kafka.KafkaTestSupport;

/**
 * @author Marius Bogoevici
 */
public class KafkaTopicMetadataStoreTest {

	public static final String TEST_KEY1 = "TEST_KEY1";

	public static final String TEST_KEY2 = "TEST_KEY2";

	public static final String TEST_KEY3 = "TEST_KEY3";

	@Rule
	public KafkaTestSupport kafkaTestSupport = new KafkaTestSupport();

	@Test
	public void testKafkaTopicMetadataStore() throws Exception {

		org.apache.log4j.LogManager.getLogger(KafkaTopicMetadataStore.class).setLevel(Level.TRACE);

		KafkaTopicMetadataStore kafkaTopicMetadataStore = createKafkaTopicMetadataStore();

		Assert.assertThat("A value cannot be retrieved if not set",
				kafkaTopicMetadataStore.get(TEST_KEY1), isEmptyOrNullString());
		kafkaTopicMetadataStore.put(TEST_KEY2, "2000");
		Assert.assertThat("A set value is retrieved correctly",
				kafkaTopicMetadataStore.get(TEST_KEY2), equalTo("2000"));
		kafkaTopicMetadataStore.put(TEST_KEY2, "2001");
		Assert.assertThat("An updated value is retrieved correctly",
				kafkaTopicMetadataStore.get(TEST_KEY2), equalTo("2001"));
		kafkaTopicMetadataStore.close();

		KafkaTopicMetadataStore secondKafkaTopicMetadataStore = createKafkaTopicMetadataStore();
		Assert.assertThat("The last set value is always retrieved for a key",
				secondKafkaTopicMetadataStore.get(TEST_KEY2), equalTo("2001"));
		secondKafkaTopicMetadataStore.put(TEST_KEY3, "3000");
		Assert.assertThat("A set value is retrieved correctly",
				secondKafkaTopicMetadataStore.get(TEST_KEY3), equalTo("3000"));
		secondKafkaTopicMetadataStore.close();


		KafkaTopicMetadataStore thirdKafkaTopicMetadataStore = createKafkaTopicMetadataStore();
		Assert.assertThat("The last set value is always retrieved for a key",
				thirdKafkaTopicMetadataStore.get(TEST_KEY2), equalTo("2001"));
		Assert.assertThat("The last set value is always retrieved for a key",
				thirdKafkaTopicMetadataStore.get(TEST_KEY3), equalTo("3000"));
		thirdKafkaTopicMetadataStore.remove(TEST_KEY3);
		thirdKafkaTopicMetadataStore.close();

		KafkaTopicMetadataStore fourthKafkaTopicMetadataStore = createKafkaTopicMetadataStore();
		Assert.assertThat("The last set value is always retrieved for a key",
				fourthKafkaTopicMetadataStore.get(TEST_KEY2), equalTo("2001"));
		Assert.assertThat("A value cannot be retrieved if not set",
				fourthKafkaTopicMetadataStore.get(TEST_KEY3), isEmptyOrNullString());
		fourthKafkaTopicMetadataStore.close();

	}

	private KafkaTopicMetadataStore createKafkaTopicMetadataStore() throws Exception {
		ZookeeperConnect zookeeperConnect = new ZookeeperConnect();
		zookeeperConnect.setZkConnect(kafkaTestSupport.getZkconnectstring());
		DefaultConnectionFactory connectionFactory =
				new DefaultConnectionFactory(new ZookeeperConfiguration(zookeeperConnect));
		connectionFactory.afterPropertiesSet();
		KafkaTopicMetadataStore kafkaTopicMetadataStore
				= new KafkaTopicMetadataStore(zookeeperConnect, connectionFactory, "offsets");

		kafkaTopicMetadataStore.afterPropertiesSet();
		return kafkaTopicMetadataStore;
	}
}
