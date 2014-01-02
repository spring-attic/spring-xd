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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.stream.DeploymentMessageSender;
import org.springframework.xd.dirt.stream.StreamRepository;
import org.springframework.xd.dirt.stream.memory.InMemoryStreamDefinitionRepository;
import org.springframework.xd.dirt.stream.memory.InMemoryStreamRepository;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

/**
 * Tests REST compliance of streams-related end-points. Contrary to {@link StreamsControllerIntegrationTests}, instead
 * of mocks, this class provides access to an actual repository: {@link InMemoryStreamRepository} and
 * {@link InMemoryStreamDefinitionRepository}.
 * 
 * @author Gunnar Hillert
 * @author Glenn Renfro
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, Dependencies.class })
public class StreamsControllerIntegrationWithRepositoryTests extends AbstractControllerIntegrationTest {

	@Autowired
	private DeploymentMessageSender sender;

	@Autowired
	protected StreamRepository streamRepository;

	@Autowired
	private ModuleRegistry moduleRegistry;

	@Before
	public void before() {
		Resource resource = new DescriptiveResource("dummy");
		ModuleDefinition sinkDefinition = new ModuleDefinition("sink",
				ModuleType.sink, resource);
		ModuleDefinition sourceDefinition = new ModuleDefinition("source",
				ModuleType.source, resource);

		ArrayList<ModuleDefinition> definitions = new ArrayList<ModuleDefinition>();
		definitions.add(new ModuleDefinition("source", ModuleType.source, resource));
		when(moduleRegistry.findDefinitions("source")).thenReturn(definitions);
		when(moduleRegistry.findDefinitions("time")).thenReturn(definitions);
		when(moduleRegistry.findDefinition("time", ModuleType.source)).thenReturn(sourceDefinition);

		definitions = new ArrayList<ModuleDefinition>();
		definitions.add(new ModuleDefinition("sink", ModuleType.sink, resource));
		when(moduleRegistry.findDefinitions("sink")).thenReturn(definitions);
		when(moduleRegistry.findDefinitions("log")).thenReturn(definitions);
		when(moduleRegistry.findDefinition("log", ModuleType.sink)).thenReturn(sinkDefinition);

		definitions = new ArrayList<ModuleDefinition>();
		definitions.add(new ModuleDefinition("processor", ModuleType.processor,
				resource));
		when(moduleRegistry.findDefinitions("processor")).thenReturn(definitions);

	}

	@Test
	public void testDeleteUnknownStream() throws Exception {
		mockMvc.perform(delete("/streams/not-there")).andExpect(status().isNotFound());
	}

	@Test
	public void testCreateUndeployAndDeleteOfStream() throws Exception {
		mockMvc.perform(
				post("/streams").param("name", "mystream").param("definition", "time | log").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		verify(sender, times(1)).sendDeploymentRequests(eq("mystream"), anyListOf(ModuleDeploymentRequest.class));

		assertNotNull(streamDefinitionRepository.findOne("mystream"));
		assertNotNull(streamRepository.findOne("mystream"));

		mockMvc.perform(put("/streams/mystream").param("deploy", "false").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk());

		verify(sender, times(2)).sendDeploymentRequests(eq("mystream"), anyListOf(ModuleDeploymentRequest.class));

		assertNotNull(streamDefinitionRepository.findOne("mystream"));
		assertNull(streamRepository.findOne("mystream"));

		mockMvc.perform(delete("/streams/mystream").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

		// As already undeployed, no new ModuleDeploymentRequest expected
		verify(sender, times(2)).sendDeploymentRequests(eq("mystream"), anyListOf(ModuleDeploymentRequest.class));
		assertNull(streamDefinitionRepository.findOne("mystream"));
		assertNull(streamRepository.findOne("mystream"));

		mockMvc.perform(delete("/streams/mystream").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());

		// As already undeployed, no new ModuleDeploymentRequest expected
		verify(sender, times(2)).sendDeploymentRequests(eq("mystream"), anyListOf(ModuleDeploymentRequest.class));
	}
}
