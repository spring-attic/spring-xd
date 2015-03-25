/*
 * Copyright 2013-2014 the original author or authors.
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.StreamDeployer;
import org.springframework.xd.dirt.stream.StreamRepository;
import org.springframework.xd.dirt.stream.zookeeper.ZooKeeperStreamDefinitionRepository;
import org.springframework.xd.dirt.stream.zookeeper.ZooKeeperStreamRepository;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.TestModuleDefinitions;

/**
 * Tests REST compliance of streams-related end-points. Unlike {@link StreamsControllerIntegrationTests}, instead of
 * mocks, this class provides access to actual repositories: {@link ZooKeeperStreamRepository} and
 * {@link ZooKeeperStreamDefinitionRepository}.
 *
 * @author Gunnar Hillert
 * @author Glenn Renfro
 * @author David Turanski
 * @author Florent Biville
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {RestConfiguration.class, Dependencies.class})
public class StreamsControllerIntegrationWithRepositoryTests extends AbstractControllerIntegrationTest {

	@Autowired
	protected StreamRepository streamRepository;

	@Autowired
	private StreamDeployer deployer;

	@Autowired
	private ModuleRegistry moduleRegistry;

	@Before
	public void before() {
		ModuleDefinition sinkDefinition = TestModuleDefinitions.dummy("sink", ModuleType.sink);
		ModuleDefinition sourceDefinition = TestModuleDefinitions.dummy("source", ModuleType.source);

		ArrayList<ModuleDefinition> definitions = new ArrayList<ModuleDefinition>();
		definitions.add(TestModuleDefinitions.dummy("source", ModuleType.source));
		when(moduleRegistry.findDefinitions("source")).thenReturn(definitions);
		when(moduleRegistry.findDefinitions("time")).thenReturn(definitions);
		when(moduleRegistry.findDefinition("time", ModuleType.source)).thenReturn(sourceDefinition);

		definitions = new ArrayList<ModuleDefinition>();
		definitions.add(TestModuleDefinitions.dummy("sink", ModuleType.sink));
		when(moduleRegistry.findDefinitions("sink")).thenReturn(definitions);
		when(moduleRegistry.findDefinitions("log")).thenReturn(definitions);
		when(moduleRegistry.findDefinition("log", ModuleType.sink)).thenReturn(sinkDefinition);

		definitions = new ArrayList<ModuleDefinition>();
		definitions.add(TestModuleDefinitions.dummy("processor", ModuleType.processor));
		when(moduleRegistry.findDefinitions("processor")).thenReturn(definitions);
	}

	@Test
	public void testDeleteUnknownStream() throws Exception {
		mockMvc.perform(delete("/streams/definitions/not-there")).andExpect(status().isNotFound());
	}

	@Test
	public void testDeployAndUndeployOfStream() throws Exception {
		mockMvc.perform(
				post("/streams/definitions").param("name", "mystream").param("definition", "time | log").param(
						"deploy", "false").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		mockMvc.perform(
				post("/streams/deployments/mystream").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		mockMvc.perform(delete("/streams/deployments/mystream").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk());

	}

	@Test
	public void testCreateUndeployAndDeleteOfStream() throws Exception {
		mockMvc.perform(
				post("/streams/definitions").param("name", "mystream").param("definition", "time | log").param(
						"deploy", "true")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		StreamDefinition definition = streamDefinitionRepository.findOne("mystream");
		assertNotNull(definition);
		assertNotNull(streamRepository.findOne("mystream"));

		mockMvc.perform(delete("/streams/deployments/mystream").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk());

		StreamDefinition undeployedDefinition = streamDefinitionRepository.findOne("mystream");
		assertNotNull(undeployedDefinition);
		assertNull(streamRepository.findOne("mystream"));

		mockMvc.perform(delete("/streams/definitions/mystream").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk());

		assertNull(streamDefinitionRepository.findOne("mystream"));
		assertNull(streamRepository.findOne("mystream"));

		mockMvc.perform(delete("/streams/definitions/mystream").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isNotFound());
	}

	@Test
	public void testCreatedUndeployedStreamIsExposedAsUndeployed() throws Exception {
		mockMvc.perform(
				post("/streams/definitions").param("name", "mystream").param("definition", "time | log").param(
						"deploy", "false")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		mockMvc.perform(get("/streams/definitions/mystream")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", Matchers.equalTo(DeploymentUnitStatus.State.undeployed.toString())));

		mockMvc.perform(delete("/streams/definitions/mystream").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk());

		assertNull(streamDefinitionRepository.findOne("mystream"));
		assertNull(streamRepository.findOne("mystream"));

		mockMvc.perform(delete("/streams/definitions/mystream").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isNotFound());

	}

	@Test
	public void testCreatedAndDeployedStreamIsExposedAsDeployed() throws Exception {
		mockMvc.perform(
				post("/streams/definitions").param("name", "mystream").param("definition", "time | log").param(
						"deploy", "true")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		mockMvc.perform(get("/streams/definitions/mystream")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", Matchers.equalTo(DeploymentUnitStatus.State.deploying.toString())));

		mockMvc.perform(delete("/streams/definitions/mystream").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk());

		assertNull(streamDefinitionRepository.findOne("mystream"));
		assertNull(streamRepository.findOne("mystream"));

		mockMvc.perform(delete("/streams/definitions/mystream").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isNotFound());

	}
}
