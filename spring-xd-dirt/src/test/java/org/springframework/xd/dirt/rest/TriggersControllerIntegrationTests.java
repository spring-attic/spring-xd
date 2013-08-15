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

package org.springframework.xd.dirt.rest;

import java.util.ArrayList;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.stream.DeploymentMessageSender;
import org.springframework.xd.dirt.stream.TriggerDefinition;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests REST compliance of taps-related endpoints.
 * 
 * @author Eric Bottard
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * 
 * @since 1.0
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, Dependencies.class })
public class TriggersControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Autowired
	private DeploymentMessageSender sender;

	@Autowired
	private TriggersController triggerController;

	@Autowired
	private ModuleRegistry moduleRegistry;

	@Before
	public void before() {
		Resource resource = mock(Resource.class);
		ModuleDefinition triggerDefinition = new ModuleDefinition(ModuleType.TRIGGER.getTypeName(),
				ModuleType.TRIGGER.getTypeName(), resource);
		ArrayList<ModuleDefinition> definitions = new ArrayList<ModuleDefinition>();
		definitions.add(triggerDefinition);
		when(moduleRegistry.findDefinitions("trigger1")).thenReturn(definitions);
		when(moduleRegistry.findDefinitions("triggerLast")).thenReturn(definitions);
		when(moduleRegistry.findDefinitions("triggerFirst")).thenReturn(definitions);
		when(moduleRegistry.findDefinitions("trigger")).thenReturn(definitions);

		when(moduleRegistry.lookup("trigger1", "trigger")).thenReturn(triggerDefinition);
		when(moduleRegistry.lookup("triggerLast", ModuleType.TRIGGER.getTypeName())).thenReturn(triggerDefinition);
		when(moduleRegistry.lookup("triggerFirst", ModuleType.TRIGGER.getTypeName())).thenReturn(triggerDefinition);
		when(moduleRegistry.lookup("trigger", ModuleType.TRIGGER.getTypeName())).thenReturn(triggerDefinition);

	}

	private final String TRIGGER_DEFINITION = "trigger --cron='*/10 * * * * *'";

	@Test
	public void testGetTrigger() throws Exception {

		triggerDefinitionRepository.save(new TriggerDefinition("trigger1", TRIGGER_DEFINITION));

		mockMvc.perform(get("/triggers/trigger1").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(
				jsonPath("$.name").value("trigger1"));
	}

	@Test
	public void testGetNonExistingTrigger() throws Exception {

		mockMvc.perform(get("/triggers/trigger1").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
	}

	@Test
	public void testListAllTriggers() throws Exception {

		mockMvc.perform(
				post("/triggers").param("name", "triggerlast").param("definition", TRIGGER_DEFINITION).accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		mockMvc.perform(
				post("/triggers").param("name", "triggerfirst").param("definition", TRIGGER_DEFINITION).accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		mockMvc.perform(get("/triggers").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(2))).andExpect(
				jsonPath("$.content[0].name").value("triggerfirst")).andExpect(
				jsonPath("$.content[1].name").value("triggerlast"));
	}

	@Test
	public void testSuccessfulTriggerCreation() throws Exception {
		mockMvc.perform(
				post("/triggers").param("name", "trigger1").param("definition", TRIGGER_DEFINITION).accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
	}

	@Test
	public void testSuccessfulTriggerCreateAndDeploy() throws Exception {
		mockMvc.perform(
				post("/triggers").param("name", "trigger1").param("definition", TRIGGER_DEFINITION).accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		verify(sender, times(1)).sendDeploymentRequests(eq("trigger1"), anyListOf(ModuleDeploymentRequest.class));
	}

	@Test
	public void testSuccessfulTriggerDeploy() throws Exception {
		triggerDefinitionRepository.save(new TriggerDefinition("trigger1", TRIGGER_DEFINITION));
		mockMvc.perform(put("/triggers/trigger1").param("deploy", "true").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk());
		verify(sender, times(1)).sendDeploymentRequests(eq("trigger1"), anyListOf(ModuleDeploymentRequest.class));
	}
	
	@Test
	public void testTriggerDestroyAll() throws Exception{
		mockMvc.perform(
				post("/triggers").param("name", "trigger1").param("definition", TRIGGER_DEFINITION).accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		mockMvc.perform(
				post("/triggers").param("name", "trigger2").param("definition", TRIGGER_DEFINITION).accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		assertNotNull(triggerDefinitionRepository.findOne("trigger1"));
		assertNotNull(triggerDefinitionRepository.findOne("trigger2"));
		
		// Perform destroy all
		mockMvc.perform(delete("/triggers").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
		
		assertNull(triggerDefinitionRepository.findOne("trigger1"));
		assertNull(triggerDefinitionRepository.findOne("trigger2"));
		
	}

	@Before
	public void resetAdditionalMocks() {
		reset(sender);
	}
}
