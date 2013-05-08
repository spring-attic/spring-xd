/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.flow.config.xml;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.flow.FlowConstants;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.AbstractRequestResponseScenarioTests;
import org.springframework.integration.test.support.MessageValidator;
import org.springframework.integration.test.support.RequestResponseScenario;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author David Turanski
 *
 */
@ContextConfiguration
public class FlowContextTests extends AbstractRequestResponseScenarioTests {

	RequestResponseScenario basicFlowScenario(String name) {
		RequestResponseScenario scenario = new RequestResponseScenario(name + ".input", name + ".output").setName(name)
				.setMessage(new GenericMessage<String>("hello")).setResponseValidator(new MessageValidator() {

					@Override
					protected void validateMessage(Message<?> msg) {
						assertEquals("hello", msg.getPayload());
						assertEquals("output", msg.getHeaders().get(FlowConstants.FLOW_OUTPUT_CHANNEL_HEADER));
					}
				});
		return scenario;
	}
	
	RequestResponseScenario profileScenario(final String profile) {
		RequestResponseScenario scenario = new RequestResponseScenario(profile + ".input", profile + ".output").setName(profile)
				.setMessage(new GenericMessage<String>("hello")).setResponseValidator(new MessageValidator() {

					@Override
					protected void validateMessage(Message<?> msg) {
						assertEquals(profile, msg.getHeaders().get("profile"));
					}
				});
		return scenario;
	}

	@Override
	protected List<RequestResponseScenario> defineRequestResponseScenarios() {
		List<RequestResponseScenario> scenarios = new ArrayList<RequestResponseScenario>();
		scenarios.add(basicFlowScenario("simple"));
		scenarios.add(profileScenario("p1"));
		scenarios.add(basicFlowScenario("p2"));
		return scenarios;
	}

	@Autowired
	@Qualifier("optional-response.output")
	SubscribableChannel optionalResponseOut;

	@Autowired
	@Qualifier("optional-response.input")
	MessageChannel optionalResponseIn;

	@Test
	public void testOptionalResponse() {
		final AtomicInteger count = new AtomicInteger();
		optionalResponseOut.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				count.getAndIncrement();
			}
		});

		for (int i = 0; i < 10; i++) {
			Message<String> msg = MessageBuilder.withPayload("hello")
					.copyHeaders(Collections.singletonMap("sendReply", i % 2 == 1)).build();
			optionalResponseIn.send(msg);
		}
		assertEquals(5, count.get());
	}

	@Autowired
	@Qualifier("router.output")
	SubscribableChannel routerOut;

	@Autowired
	@Qualifier("router.input")
	MessageChannel routerIn;

	@Test
	public void testRouter() {
		final AtomicInteger fooCount = new AtomicInteger();
		final AtomicInteger barCount = new AtomicInteger();
		routerOut.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> msg) {
				assertEquals("output." + msg.getPayload(),
						msg.getHeaders().get(FlowConstants.FLOW_OUTPUT_CHANNEL_HEADER));
				if ("foo".equals(msg.getPayload())) {
					fooCount.getAndIncrement();
				}
				if ("bar".equals(msg.getPayload())) {
					barCount.getAndIncrement();
				}
			}
		});
		for (int i = 0; i < 10; i++) {
			Message<String> msg = MessageBuilder.withPayload(i%2==0? "foo" : "bar")
			.build();
			routerIn.send(msg);
		}
		assertEquals(5,fooCount.get());
		assertEquals(5,barCount.get());
	}
}
