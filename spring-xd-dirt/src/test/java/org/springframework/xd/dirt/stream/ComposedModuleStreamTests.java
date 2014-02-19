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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.xd.dirt.module.DependencyException;
import org.springframework.xd.dirt.module.NoSuchModuleException;
import org.springframework.xd.dirt.rest.DetailedModuleDefinitionResourceAssembler;
import org.springframework.xd.dirt.rest.ModulesController;
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
		// Delete the stream definition
		deleteStream("streamWithCompositeSource");
	}

	@Test
	public void testDeleteCompositeSource() {
		deployStream("aCompositeStream", "compositesource | sink");
		CompositeModuleDefinitionService compositeModuleDefinitionService = getAdminContext().getBean(
				CompositeModuleDefinitionService.class);
		// Not actually used in test so ok for now
		DetailedModuleDefinitionResourceAssembler assembler = mock(DetailedModuleDefinitionResourceAssembler.class);
		ModulesController modulesController = new ModulesController(compositeModuleDefinitionService, assembler);
		try {
			modulesController.delete(ModuleType.source, "compositeModuleThatDoesNotExist");
			fail("Exception should be thrown when trying to delete a composite module that does not exist.");
		}
		catch (NoSuchModuleException e) {
			assertEquals("Could not find module with name 'compositeModuleThatDoesNotExist' and type 'source'",
					e.getMessage());
		}
		try {
			modulesController.delete(ModuleType.source, "source");
			fail("Exception should be thrown when trying to delete a non-composite module.");
		}
		catch (IllegalStateException e) {
			assertEquals("Cannot delete non-composed module source:source", e.getMessage());
		}
		assertDeleteFailsWhenPartOfStreamDef(modulesController);
		undeployStream("aCompositeStream");
		// Delete will still fail after undeploying the stream
		assertDeleteFailsWhenPartOfStreamDef(modulesController);
		// Delete the stream defintion
		deleteStream("aCompositeStream");
		// Now delete the composite module
		modulesController.delete(ModuleType.source, "compositesource");
		// Assert that it was deleted
		assertNull(getModuleDefinitionRepository().findByNameAndType("compositesource", ModuleType.source));
	}

	private void assertDeleteFailsWhenPartOfStreamDef(ModulesController modulesController) {
		try {
			modulesController.delete(ModuleType.source, "compositesource");
			fail("Should not be able to delete composite module if it is part of a stream definition");
		}
		catch (DependencyException e) {
			assertEquals("name of composite module not as expected", "compositesource", e.getName());
		}
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
		// Delete the stream definition
		deleteStream("streamWithCompositeProcessor");
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
		// Delete the stream definition
		deleteStream("streamWithCompositeSink");
	}

	private static void composeModule(String name, String definition, ModuleType type) {
		ModuleDefinition moduleDefinition = new ModuleDefinition(name, type);
		moduleDefinition.setDefinition(definition);
		getModuleDefinitionRepository().save(moduleDefinition);
	}

}
