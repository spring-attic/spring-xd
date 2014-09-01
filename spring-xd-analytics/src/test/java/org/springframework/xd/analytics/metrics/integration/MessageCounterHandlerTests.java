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

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.analytics.metrics.core.CounterRepository;
import org.springframework.xd.analytics.metrics.memory.InMemoryCounterRepository;

/**
 * @author Mark Pollack
 * @author Gary Russell
 * @since 1.0
 *
 */
public class MessageCounterHandlerTests {

	private CounterRepository repo = new InMemoryCounterRepository();

	@Test
	public void testNoPreexistingCounter() {
		MessageCounterHandler handler = new MessageCounterHandler(repo, "'tupleCounter'");
		Message<String> message = MessageBuilder.withPayload("Hi").build();
		handler.process(message);
		Counter counter = repo.findOne("tupleCounter");
		assertThat(counter.getValue(), equalTo(1L));

	}

	@Test
	public void testPreexistingCounter() {
		Counter counter = new Counter("tupleCounter", 12L);
		repo.save(counter);
		MessageCounterHandler handler = new MessageCounterHandler(repo, "'tupleCounter'");
		Message<String> message = MessageBuilder.withPayload("Hi").build();
		handler.process(message);
		counter = repo.findOne("tupleCounter");
		assertThat(counter.getValue(), equalTo(13L));

	}

	@Test
	public void testNullMsgHasNoEffect() {
		Counter counter = new Counter("tupleCounter", 12L);
		repo.save(counter);
		MessageCounterHandler handler = new MessageCounterHandler(repo, "'tupleCounter'");
		handler.process(null);
		counter = repo.findOne("tupleCounter");
		assertThat(counter.getValue(), equalTo(12L));
	}

}
