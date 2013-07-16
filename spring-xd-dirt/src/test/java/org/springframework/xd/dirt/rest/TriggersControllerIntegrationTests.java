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

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.stream.TriggerDefinition;
import org.springframework.xd.dirt.stream.TriggerDeploymentMessageSender;

/**
 * Tests REST compliance of taps-related endpoints.
 *
 * @author Eric Bottard
 * @author David Turanski
 * @author Gunnar Hillert
 *
 * @since 1.0
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, MockedDependencies.class,
		TriggersControllerIntegrationTestsConfig.class })
public class TriggersControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Autowired
	private TriggerDeploymentMessageSender sender;

	@Autowired
	private TriggersController triggerController;

	private final String TRIGGER_DEFINITION = "trigger --cron='*/10 * * * * *'";


	@Test
	public void testGetTrigger() throws Exception {

		triggerDefinitionRepository.save(new TriggerDefinition("trigger1", TRIGGER_DEFINITION));

		mockMvc.perform(get("/triggers/trigger1")
			.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("trigger1"));
	}

	@Test
	public void testGetNonExistingTrigger() throws Exception {

		mockMvc.perform(get("/triggers/trigger1")
			.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound());
	}

	@Test
	public void testListAllTriggers() throws Exception {

		mockMvc.perform(
				post("/triggers").param("name", "triggerlast").param("definition", TRIGGER_DEFINITION)
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		mockMvc.perform(
				post("/triggers").param("name", "triggerfirst").param("definition", TRIGGER_DEFINITION)
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		mockMvc.perform(get("/triggers").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$", Matchers.hasSize(2))).andExpect(jsonPath("$.[0].name").value("triggerfirst"))
				.andExpect(jsonPath("$.[1].name").value("triggerlast"));
	}

	@Test
	public void testSuccessfulTriggerCreation() throws Exception {
		mockMvc.perform(
				post("/triggers").param("name", "trigger1").param("definition", TRIGGER_DEFINITION)
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
	}

	@Test
	public void testSuccessfulTriggerCreateAndDeploy() throws Exception {
		mockMvc.perform(
				post("/triggers").param("name", "trigger1").param("definition", TRIGGER_DEFINITION)
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		verify(sender, times(1)).sendDeploymentRequests(eq("trigger1"), anyListOf(ModuleDeploymentRequest.class));
	}

	@Test
	public void testSuccessfulTriggerDeploy() throws Exception {
		triggerDefinitionRepository.save(new TriggerDefinition("trigger1", TRIGGER_DEFINITION));
		mockMvc.perform(put("/triggers/trigger1").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
		verify(sender, times(1)).sendDeploymentRequests(eq("trigger1"), anyListOf(ModuleDeploymentRequest.class));
	}

	@Before
	public void resetAdditionalMocks() {
		reset(sender);
	}
}
