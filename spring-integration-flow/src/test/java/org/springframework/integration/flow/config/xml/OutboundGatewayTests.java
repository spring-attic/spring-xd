/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author David Turanski
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class OutboundGatewayTests {
	@Autowired
	MessageChannel gwInput;
	@Autowired
	SubscribableChannel gwOutput;
	@Autowired
	MessageChannel anotherInput;
	@Autowired
	SubscribableChannel anotherOutput;
	
	
	@Test
	public void testSharedGateway() throws InterruptedException {
		CountHandler gwHandler = new CountHandler("hello");
		CountHandler anotherHandler = new CountHandler("world");
		gwOutput.subscribe(gwHandler);
		anotherOutput.subscribe(anotherHandler);
		
		Thread firstThread = new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i< 100; i++) {
					gwInput.send(new GenericMessage<String>("hello"));
				}
			}
		});
		Thread secondThread = new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i< 100; i++) {
					anotherInput.send(new GenericMessage<String>("world"));
				}
			}
		});
		
		firstThread.start();
		secondThread.start();
		firstThread.join();
		secondThread.join();
		assertEquals(100,gwHandler.count);
		assertEquals(100,anotherHandler.count);
	}

	@Autowired
	MessageChannel nestedInput;
	@Autowired
	SubscribableChannel nestedOutput;
	@Test
	public void testNestedFlows() {
		CountHandler handler = new CountHandler("hello");
		nestedOutput.subscribe(handler);
		nestedInput.send(new GenericMessage<String>("hello"));
		assertEquals(1,handler.count);
		
	}

	@Autowired
	MessageChannel exceptionInput;
	@Autowired
	SubscribableChannel errorChannel;
	@Test
	public void testFlowWithErrorChannel() {
		final AtomicBoolean received = new AtomicBoolean();
		errorChannel.subscribe(new MessageHandler() {
			
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				assertTrue(message instanceof ErrorMessage);
				received.set(true);
			}
		});
		exceptionInput.send(new GenericMessage<String>("hello"));
		assertTrue(received.get());
	}

	static class CountHandler implements MessageHandler {
		public int count;
		private final String expectedPayload;
		public CountHandler(String expectedPayload) {
			this.expectedPayload = expectedPayload;
		}
		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			assertEquals(this.expectedPayload, message.getPayload());
			count++;
		}
	}
}
