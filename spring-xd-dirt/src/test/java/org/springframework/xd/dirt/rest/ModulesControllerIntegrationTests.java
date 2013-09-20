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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.stream.DeploymentMessageSender;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

/**
 * Tests REST compliance of module-related endpoints.
 * 
 * @author Glenn Renfro
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, Dependencies.class })
public class ModulesControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Autowired
	private DeploymentMessageSender sender;

	@Autowired
	private ModuleRegistry moduleRegistry;

	@Before
	public void before() {
		Resource resource = mock(Resource.class);
		ArrayList<ModuleDefinition> moduleDefinitions = new ArrayList<ModuleDefinition>();
		ArrayList<ModuleDefinition> jobDefinitions = new ArrayList<ModuleDefinition>();
		ArrayList<ModuleDefinition> processorDefinitions = new ArrayList<ModuleDefinition>();
		ArrayList<ModuleDefinition> sinkDefinitions = new ArrayList<ModuleDefinition>();
		ArrayList<ModuleDefinition> sourceDefinitions = new ArrayList<ModuleDefinition>();

		for (int moduleCount = 0; moduleCount < 3; moduleCount++) {

			ModuleDefinition moduleDefinition = new ModuleDefinition(ModuleType.JOB.getTypeName() + "_" + moduleCount,
					ModuleType.JOB.getTypeName(), resource);
			moduleDefinitions.add(moduleDefinition);
			jobDefinitions.add(moduleDefinition);

			moduleDefinition = new ModuleDefinition(ModuleType.SOURCE.getTypeName() + "_" + moduleCount,
					ModuleType.SOURCE.getTypeName(), resource);
			moduleDefinitions.add(moduleDefinition);
			sourceDefinitions.add(moduleDefinition);

			moduleDefinition = new ModuleDefinition(ModuleType.SINK.getTypeName() + "_" + moduleCount,
					ModuleType.SINK.getTypeName(), resource);
			moduleDefinitions.add(moduleDefinition);
			sinkDefinitions.add(moduleDefinition);

			moduleDefinition = new ModuleDefinition(ModuleType.PROCESSOR.getTypeName() + "_" + moduleCount,
					ModuleType.PROCESSOR.getTypeName(), resource);
			moduleDefinitions.add(moduleDefinition);
			processorDefinitions.add(moduleDefinition);
		}

		when(resource.exists()).thenReturn(true);
		when(resource.getDescription()).thenReturn("foo");
		when(moduleRegistry.findDefinitions(ModuleType.SINK)).thenReturn(sinkDefinitions);
		when(moduleRegistry.findDefinitions(ModuleType.SOURCE)).thenReturn(sourceDefinitions);
		when(moduleRegistry.findDefinitions(ModuleType.JOB)).thenReturn(jobDefinitions);
		when(moduleRegistry.findDefinitions(ModuleType.PROCESSOR)).thenReturn(processorDefinitions);
		when(moduleRegistry.findDefinitions()).thenReturn(moduleDefinitions);
	}

	@Test
	public void testListAll() throws Exception {
		mockMvc.perform(get("/modules").param("type", "all").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(12))).andExpect(jsonPath("$.content[0].name").value("job_0")).andExpect(
				jsonPath("$.content[1].name").value("source_0")).andExpect(
				jsonPath("$.content[11].name").value("processor_2")).andExpect(
				jsonPath("$.content[10].name").value("sink_2"));
	}

	@Test
	public void testListJob() throws Exception {
		mockMvc.perform(get("/modules").param("type", "job").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(3))).andExpect(jsonPath("$.content[0].name").value("job_0")).andExpect(
				jsonPath("$.content[1].name").value("job_1")).andExpect(
				jsonPath("$.content[2].name").value("job_2"));
	}


	@Test
	public void testListSource() throws Exception {
		mockMvc.perform(get("/modules").param("type", "source").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(3))).andExpect(jsonPath("$.content[0].name").value("source_0")).andExpect(
				jsonPath("$.content[1].name").value("source_1")).andExpect(
				jsonPath("$.content[2].name").value("source_2"));
	}

	@Test
	public void testListSink() throws Exception {
		mockMvc.perform(get("/modules").param("type", "sink").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(3))).andExpect(jsonPath("$.content[0].name").value("sink_0")).andExpect(
				jsonPath("$.content[1].name").value("sink_1")).andExpect(
				jsonPath("$.content[2].name").value("sink_2"));
	}

	@Test
	public void testListBadType() throws Exception {
		mockMvc.perform(get("/modules").param("type", "foo").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(0)));
	}
}
