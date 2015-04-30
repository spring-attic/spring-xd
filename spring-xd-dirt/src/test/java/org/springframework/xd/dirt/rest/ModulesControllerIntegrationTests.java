/*
 * Copyright 2013-2015 the original author or authors.
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.dirt.module.WritableModuleRegistry;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDefinitions;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.TestModuleDefinitions;

/**
 * Tests REST compliance of module-related endpoints.
 *
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {RestConfiguration.class, Dependencies.class})
public class ModulesControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Autowired
	private WritableModuleRegistry moduleRegistry;

	@Before
	public void setupSuccessfulModuleComposition() throws IOException {
		ModuleDefinition any = Mockito.any();
		when(moduleRegistry.registerNew(any)).thenReturn(true);

		File config = temporaryFolder.newFolder("config");
		Properties props = new Properties();
		props.setProperty("options.foo.description", "the foo property");
		props.setProperty("options.foo.type", "String");
		props.setProperty("options.foo.default", "yummy");
		props.store(new FileOutputStream(new File(config, "foo.properties")), "module properties");
	}

	@Before
	public void setupFindDefinitionResults() throws IOException {
		ArrayList<ModuleDefinition> moduleDefinitions = new ArrayList<ModuleDefinition>();
		ArrayList<ModuleDefinition> jobDefinitions = new ArrayList<ModuleDefinition>();
		ArrayList<ModuleDefinition> processorDefinitions = new ArrayList<ModuleDefinition>();
		ArrayList<ModuleDefinition> sinkDefinitions = new ArrayList<ModuleDefinition>();
		ArrayList<ModuleDefinition> sourceDefinitions = new ArrayList<ModuleDefinition>();

		for (int moduleCount = 0; moduleCount < 3; moduleCount++) {

			ModuleDefinition moduleDefinition = TestModuleDefinitions.dummy("job_" + moduleCount, ModuleType.job);
			moduleDefinitions.add(moduleDefinition);
			jobDefinitions.add(moduleDefinition);

			moduleDefinition = TestModuleDefinitions.dummy("source_" + moduleCount, ModuleType.source);
			moduleDefinitions.add(moduleDefinition);
			sourceDefinitions.add(moduleDefinition);

			moduleDefinition = TestModuleDefinitions.dummy("sink_" + moduleCount, ModuleType.sink);
			moduleDefinitions.add(moduleDefinition);
			sinkDefinitions.add(moduleDefinition);

			moduleDefinition = TestModuleDefinitions.dummy("processor_" + moduleCount, ModuleType.processor);
			moduleDefinitions.add(moduleDefinition);
			processorDefinitions.add(moduleDefinition);
		}


		ModuleDefinition moduleDefinition = ModuleDefinitions.simple("job_4_with_resource", ModuleType.job, "file:" + temporaryFolder.getRoot().getAbsolutePath() + "/");
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

	}

	@Test
	public void testListAll() throws Exception {
		mockMvc.perform(get("/modules").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(13))).andExpect(jsonPath("$.content[0].name").value("job_0")).andExpect(
				jsonPath("$.content[3].name").value("job_4_with_resource")).andExpect(
				jsonPath("$.content[6].name").value("processor_2")).andExpect(
				jsonPath("$.content[9].name").value("sink_2")).andExpect(
				jsonPath("$.content[12].name").value("source_2"));
	}

	@Test
	public void testListAllWithDetails() throws Exception {
		mockMvc.perform(get("/modules?detailed=true").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(13))).andExpect(jsonPath("$.content[0].name").value("job_0")).andExpect(
				jsonPath("$.content[3].name").value("job_4_with_resource")).andExpect(
				jsonPath("$.content[3].options[0].description").value("the foo property"));
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

	@Test
	public void testModuleInfoWithIncorrectModuleName() throws Exception {
		mockMvc.perform(get("/modules/source/test")).andExpect(status().isNotFound()).andExpect(
				jsonPath("$[0].message", Matchers.is("Could not find module with name 'test' and type 'source'")));
	}
}
