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
import org.springframework.xd.dirt.stream.DeploymentMessageSender;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.TapDefinition;

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
		TapsControllerIntegrationTestsConfig.class })
public class TapsControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Autowired
	private DeploymentMessageSender sender;

	@Autowired
	private TapsController tapsController;

	@Test
	public void testListAllTaps() throws Exception {
		streamDefinitionRepository.save(new StreamDefinition("test", "time | log"));

		mockMvc.perform(
				post("/taps").param("name", "taplast").param("definition", "tap@ test | log")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		mockMvc.perform(
				post("/taps").param("name", "tapfirst").param("definition", "tap@ test | log")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		mockMvc.perform(get("/taps").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", Matchers.hasSize(2)))
				.andExpect(jsonPath("$.content[0].name").value("tapfirst"))
				.andExpect(jsonPath("$.content[1].name").value("taplast"));
	}

	@Test
	public void testSuccessfulTapCreation() throws Exception {
		streamDefinitionRepository.save(new StreamDefinition("test", "time | log"));
		mockMvc.perform(
				post("/taps").param("name", "tap1").param("definition", "tap@ test | log")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
	}

	@Test
	public void testSuccessfulTapCreateAndDeploy() throws Exception {
		streamDefinitionRepository.save(new StreamDefinition("test", "time | log"));
		mockMvc.perform(
				post("/taps").param("name", "tap1").param("definition", "tap@ test | log")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		verify(sender, times(1)).sendDeploymentRequests(eq("tap1"), anyListOf(ModuleDeploymentRequest.class));
	}

	@Test
	public void testSuccessfulTapDeploy() throws Exception {
		streamDefinitionRepository.save(new StreamDefinition("test", "time | log"));
		tapDefinitionRepository.save(new TapDefinition("tap1", "test", "tap@test | log"));
		mockMvc.perform(put("/taps/tap1").param("deploy", "true").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
		verify(sender, times(1)).sendDeploymentRequests(eq("tap1"), anyListOf(ModuleDeploymentRequest.class));
	}

	@Test
	public void testStreamCreationNoDefinition() throws Exception {
		mockMvc.perform(post("/taps").param("name", "mystream").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isBadRequest());
	}

	@Before
	public void resetAdditionalMocks() {
		reset(sender);
	}
}
