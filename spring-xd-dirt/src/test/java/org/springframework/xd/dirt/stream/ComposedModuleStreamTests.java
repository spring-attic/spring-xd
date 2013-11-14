/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.stream;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

/**
 * Tests for streams created with composed modules.
 * 
 * @author Mark Fisher
 */
public class ComposedModuleStreamTests extends StreamTestSupport {

	@BeforeClass
	public static void setup() {
		composeModule("compositesource", "source | testprocessor1", ModuleType.source);
		composeModule("compositeprocessor", "testprocessor1 | testprocessor2", ModuleType.processor);
		composeModule("compositesink", "testprocessor2 | sink", ModuleType.sink);
	}

	@Test
	public void testStreamWithCompositeSource() {
		deployStream("streamWithCompositeSource", "compositesource | sink");
		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				assertEquals("foo1", message.getPayload());
			}
		};
		sendPayloadAndVerifyOutput("streamWithCompositeSource", "foo", test);
	}

	@Test
	public void testStreamWithCompositeProcessor() {
		deployStream("streamWithCompositeProcessor", "source | compositeprocessor | sink");
		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				assertEquals("foo12", message.getPayload());
			}
		};
		sendPayloadAndVerifyOutput("streamWithCompositeProcessor", "foo", test);
	}

	@Test
	public void testStreamWithCompositeSink() {
		deployStream("streamWithCompositeSink", "source | compositesink");
		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				assertEquals("foo2", message.getPayload());
			}
		};
		sendPayloadAndVerifyOutput("streamWithCompositeSink", "foo", test);
	}

	private static void composeModule(String name, String definition, ModuleType type) {
		ModuleDefinition moduleDefinition = new ModuleDefinition(name, type);
		moduleDefinition.setDefinition(definition);
		getModuleDefinitionRepository().save(moduleDefinition);
	}

}
