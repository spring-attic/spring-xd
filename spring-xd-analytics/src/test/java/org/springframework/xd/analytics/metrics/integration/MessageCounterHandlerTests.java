/*
 * Copyright 2013 the original author or authors.
 *
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
package org.springframework.xd.analytics.metrics.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.analytics.metrics.core.CounterRepository;
import org.springframework.xd.analytics.metrics.redis.RedisCounterService;
import org.springframework.xd.test.redis.RedisAvailableRule;

/**
 * @author Mark Pollack
 * @author Gary Russell
 * @since 1.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MessageCounterHandlerTests {

	@Rule
	public RedisAvailableRule redisAvailableRule = new RedisAvailableRule();

	@Autowired
	private RedisCounterService counterService;

	@Autowired
	private CounterRepository repo;

	@Before
	@After
	public void initAndCleanup() {
		repo.delete("tupleCounter");
	}

	@Test
	public void messageCountTest() {
		Counter counter = counterService.getOrCreate("tupleCounter");

		//Counter counter = repo.findByName("tupleCounter");
		assertThat(counter.getValue(), equalTo(0L));
		MessageCounterHandler handler = new MessageCounterHandler(counterService, "tupleCounter");
		Message<String> message = MessageBuilder.withPayload("Hi").build();

		handler.process(message);
		counter = repo.findOne("tupleCounter");
		assertThat(counter.getValue(), equalTo(1L));

	}

}
