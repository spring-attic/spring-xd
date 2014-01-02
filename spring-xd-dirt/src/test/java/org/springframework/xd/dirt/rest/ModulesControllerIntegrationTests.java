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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.stream.DeploymentMessageSender;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

/**
 * Tests REST compliance of module-related endpoints.
 * 
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Gunnar Hillert
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, Dependencies.class })
public class ModulesControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Autowired
	private DeploymentMessageSender sender;

	@Autowired
	private ModuleRegistry moduleRegistry;

	@Autowired
	private ModuleDefinitionRepository moduleDefinitionRepository;

	@Before
	public void before() throws IOException {
		Resource resource = new DescriptiveResource("dummy");
		ArrayList<ModuleDefinition> moduleDefinitions = new ArrayList<ModuleDefinition>();
		ArrayList<ModuleDefinition> jobDefinitions = new ArrayList<ModuleDefinition>();
		ArrayList<ModuleDefinition> processorDefinitions = new ArrayList<ModuleDefinition>();
		ArrayList<ModuleDefinition> sinkDefinitions = new ArrayList<ModuleDefinition>();
		ArrayList<ModuleDefinition> sourceDefinitions = new ArrayList<ModuleDefinition>();

		for (int moduleCount = 0; moduleCount < 3; moduleCount++) {

			ModuleDefinition moduleDefinition = new ModuleDefinition("job_" + moduleCount,
					ModuleType.job, resource);
			moduleDefinitions.add(moduleDefinition);
			jobDefinitions.add(moduleDefinition);

			moduleDefinition = new ModuleDefinition("source_" + moduleCount,
					ModuleType.source, resource);
			moduleDefinitions.add(moduleDefinition);
			sourceDefinitions.add(moduleDefinition);

			moduleDefinition = new ModuleDefinition("sink_" + moduleCount,
					ModuleType.sink, resource);
			moduleDefinitions.add(moduleDefinition);
			sinkDefinitions.add(moduleDefinition);

			moduleDefinition = new ModuleDefinition("processor_" + moduleCount,
					ModuleType.processor, resource);
			moduleDefinitions.add(moduleDefinition);
			processorDefinitions.add(moduleDefinition);
		}

		final File file = temporaryFolder.newFile("job_4_with_resource.xml");
		FileUtils.write(file, "This is the contents of job_4_with_resource.xml");

		ModuleDefinition moduleDefinition = new ModuleDefinition("job_4_with_resource",
				ModuleType.job, new FileSystemResource(file));
		moduleDefinitions.add(moduleDefinition);
		jobDefinitions.add(moduleDefinition);

		for (int i = 0; i < 3; i++) {
			when(moduleRegistry.findDefinition("source_" + i, ModuleType.source)).thenReturn(sourceDefinitions.get(i));
			when(moduleRegistry.findDefinitions("source_" + i)).thenReturn(
					Collections.singletonList(sourceDefinitions.get(i)));
			when(moduleRegistry.findDefinition("processor_" + i, ModuleType.processor)).thenReturn(
					processorDefinitions.get(i));
			when(moduleRegistry.findDefinitions("processor_" + i)).thenReturn(
					Collections.singletonList(processorDefinitions.get(i)));
			when(moduleRegistry.findDefinition("sink_" + i, ModuleType.sink)).thenReturn(sinkDefinitions.get(i));
			when(moduleRegistry.findDefinitions("sink_" + i)).thenReturn(
					Collections.singletonList(sinkDefinitions.get(i)));
		}
		when(moduleRegistry.findDefinitions(ModuleType.sink)).thenReturn(sinkDefinitions);
		when(moduleRegistry.findDefinitions(ModuleType.source)).thenReturn(sourceDefinitions);
		when(moduleRegistry.findDefinitions(ModuleType.job)).thenReturn(jobDefinitions);
		when(moduleRegistry.findDefinitions(ModuleType.processor)).thenReturn(processorDefinitions);
		when(moduleRegistry.findDefinitions()).thenReturn(moduleDefinitions);
		when(moduleRegistry.findDefinition("job_4_with_resource", ModuleType.job)).thenReturn(moduleDefinition);

		// clear this one so it does not have side effects on other tests
		moduleDefinitionRepository.delete("sink:compositesink");
	}

	@Test
	public void testListAll() throws Exception {
		mockMvc.perform(get("/modules").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(13))).andExpect(jsonPath("$.content[0].name").value("job_0")).andExpect(
				jsonPath("$.content[1].name").value("source_0")).andExpect(
				jsonPath("$.content[11].name").value("processor_2")).andExpect(
				jsonPath("$.content[10].name").value("sink_2"));
	}

	@Test
	public void testListJob() throws Exception {
		mockMvc.perform(get("/modules").param("type", "job").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(4))).andExpect(jsonPath("$.content[0].name").value("job_0")).andExpect(
				jsonPath("$.content[1].name").value("job_1")).andExpect(
				jsonPath("$.content[2].name").value("job_2")).andExpect(
				jsonPath("$.content[3].name").value("job_4_with_resource"));
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
				jsonPath("$.content", Matchers.hasSize(3))).andExpect(
				jsonPath("$.content[0].name").value("sink_0")).andExpect(
				jsonPath("$.content[1].name").value("sink_1")).andExpect(
				jsonPath("$.content[2].name").value("sink_2"));
	}

	@Test
	public void testDisplayNonExistingJobConfiguration() throws Exception {
		mockMvc.perform(get("/modules/job/not_exists/definition").accept(MediaType.APPLICATION_XML)).andExpect(
				status().isNotFound());
	}

	@Test
	public void testDisplayJobConfiguration() throws Exception {
		mockMvc.perform(get("/modules/job/job_4_with_resource/definition").accept(MediaType.APPLICATION_XML)).andExpect(
				status().isOk()).andExpect(content().string("This is the contents of job_4_with_resource.xml"));
	}

	@Test
	public void testListBadType() throws Exception {
		mockMvc.perform(get("/modules").param("type", "foo").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isInternalServerError());
	}

	@Test
	public void saveComposedModule() throws Exception {
		mockMvc.perform(post("/modules")
				.param("name", "compositesink")
				.param("definition", "processor_2 | sink_1")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().is(201)).andExpect(
						jsonPath("$.name").value("compositesink")).andExpect(
						jsonPath("$.type").value("sink"));
	}

}
