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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.dirt.core.DeploymentUnitStatus.State;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.module.store.ModuleMetadata;
import org.springframework.xd.dirt.stream.Job;
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.Stream;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.TestModuleDefinitions;

/**
 * Tests REST compliance of module metadata endpoint.
 *
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {RestConfiguration.class, Dependencies.class})
public class ModuleMetadataControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Autowired
	private ModuleRegistry moduleRegistry;

	@Before
	public void before() throws Exception {
		PageRequest pageable = new PageRequest(0, 20);
		Properties entityProps1 = new Properties();
		entityProps1.put("entity1", "value1");
		Properties entityProps2 = new Properties();
		entityProps2.put("entity2", "value2");
		Properties entityProps3 = new Properties();
		entityProps3.put("entity3", "value3");
		Properties deploymentProps1 = new Properties();
		deploymentProps1.put("count", "1");
		Properties deploymentProps2 = new Properties();
		deploymentProps2.put("count", "2");
		Properties deploymentProps3 = new Properties();
		deploymentProps3.put("criteria", "groups.contains('hdfs')");
		Stream stream1 = new Stream(new StreamDefinition("s1", "http | log"));
		stream1.setStatus(new DeploymentUnitStatus(State.deployed));
		ModuleMetadata entity1 = new ModuleMetadata(new ModuleMetadata.Id("1", "s1.source.http.0"),
				entityProps1, deploymentProps1, State.deployed);
		Stream stream2 = new Stream(new StreamDefinition("s2", "http | log"));
		stream1.setStatus(new DeploymentUnitStatus(State.failed));

		ModuleMetadata entity2 = new ModuleMetadata(new ModuleMetadata.Id("2", "s2.sink.log.1"),
				entityProps2, deploymentProps2, DeploymentUnitStatus.State.deployed);

		Job job1 = new Job(new JobDefinition("j3", "job"));
		job1.setStatus(new DeploymentUnitStatus(State.deployed));

		ModuleMetadata entity3 = new ModuleMetadata(new ModuleMetadata.Id("3", "j3.job.myjob.0"),
				entityProps3, deploymentProps3, State.deployed);
		List<ModuleMetadata> entities1 = new ArrayList<ModuleMetadata>();
		List<ModuleMetadata> entities2 = new ArrayList<ModuleMetadata>();
		List<ModuleMetadata> entities3 = new ArrayList<ModuleMetadata>();
		List<ModuleMetadata> all = new ArrayList<ModuleMetadata>();
		entities1.add(entity1);
		entities2.add(entity2);
		entities3.add(entity3);
		all.add(entity1);
		all.add(entity2);
		all.add(entity3);
		Page<ModuleMetadata> allPages = new PageImpl<>(all);
		Page<ModuleMetadata> pageEntity2 = new PageImpl<>(entities2);
		Page<ModuleMetadata> pageEntity3 = new PageImpl<>(entities3);
		when(moduleMetadataRepository.findAll(pageable)).thenReturn(allPages);
		when(moduleMetadataRepository.findAll()).thenReturn(all);
		when(moduleMetadataRepository.findAllByContainerId(pageable, "2")).thenReturn(pageEntity2);
		when(moduleMetadataRepository.findOne("1", "s1.source.http.0")).thenReturn(entity1);
		when(moduleMetadataRepository.findAllByModuleId(pageable, "j3.job.myjob.0")).thenReturn(pageEntity3);
		ModuleDefinition sinkDefinition = TestModuleDefinitions.dummy("sink", ModuleType.sink);
		ModuleDefinition sourceDefinition = TestModuleDefinitions.dummy("source", ModuleType.source);
		ModuleDefinition jobDefinition = TestModuleDefinitions.dummy("job", ModuleType.job);

		ArrayList<ModuleDefinition> definitions = new ArrayList<ModuleDefinition>();
		definitions.add(TestModuleDefinitions.dummy("source", ModuleType.source));
		when(moduleRegistry.findDefinitions("source")).thenReturn(definitions);
		when(moduleRegistry.findDefinitions("http")).thenReturn(definitions);
		when(moduleRegistry.findDefinition("http", ModuleType.source)).thenReturn(sourceDefinition);

		definitions = new ArrayList<ModuleDefinition>();
		definitions.add(TestModuleDefinitions.dummy("sink", ModuleType.sink));
		when(moduleRegistry.findDefinitions("sink")).thenReturn(definitions);
		when(moduleRegistry.findDefinitions("log")).thenReturn(definitions);
		when(moduleRegistry.findDefinition("log", ModuleType.sink)).thenReturn(sinkDefinition);

		definitions.add(TestModuleDefinitions.dummy("job", ModuleType.job));
		when(moduleRegistry.findDefinitions("job")).thenReturn(definitions);
		when(moduleRegistry.findDefinitions("job")).thenReturn(definitions);
		when(moduleRegistry.findDefinition("job", ModuleType.job)).thenReturn(jobDefinition);
		mockMvc.perform(
				post("/streams/definitions").param("name", stream1.getDefinition().getName()).param("definition",
						"http | log").param(
						"deploy", "true")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		assertNotNull(streamRepository.findOne(stream1.getDefinition().getName()));
		mockMvc.perform(
				post("/streams/definitions").param("name", stream2.getDefinition().getName()).param("definition",
						"http | log").param(
						"deploy", "true")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		assertNotNull(streamRepository.findOne(stream2.getDefinition().getName()));
		mockMvc.perform(
				post("/jobs/definitions").param("name", job1.getDefinition().getName()).param("definition",
						"job").param(
						"deploy", "true")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		assertNotNull(jobRepository.findOne(job1.getDefinition().getName()));

	}

	@Test
	public void testListModules() throws Exception {
		mockMvc.perform(get("/runtime/modules").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(3))).andExpect(
				jsonPath("$.content[*].moduleId", contains("s1.source.http.0", "s2.sink.log.1", "j3.job.myjob.0"))).andExpect(
				jsonPath("$.content[*].name", contains("http.0", "log.1", "myjob.0"))).andExpect(
				jsonPath("$.content[*].unitName", contains("s1", "s2", "j3"))).andExpect(
				jsonPath("$.content[*].moduleType", contains("source", "sink", "job"))).andExpect(
				jsonPath("$.content[*].containerId", contains("1", "2", "3"))).andExpect(
				jsonPath("$.content[*].moduleOptions", Matchers.hasSize(3))).andExpect(
				jsonPath("$.content[*].deploymentProperties", Matchers.hasSize(3)));
	}

	@Test
	public void testListModulesByJobName() throws Exception {
		mockMvc.perform(get("/runtime/modules?jobname=j3").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$", Matchers.hasSize(1))).andExpect(
				jsonPath("$[*].moduleId", contains("j3.job.myjob.0"))).andExpect(
				jsonPath("$[*].name", contains("myjob.0"))).andExpect(
				jsonPath("$[*].containerId", contains("3"))).andExpect(
				jsonPath("$[*].moduleType", contains("job"))).andExpect(
				jsonPath("$[*].moduleOptions.entity3", contains("value3"))).andExpect(
				jsonPath("$[*].deploymentProperties.criteria", contains("groups.contains('hdfs')")));
	}

	@Test
	public void testListModuleByModuleId() throws Exception {
		mockMvc.perform(get("/runtime/modules?moduleId=j3.job.myjob.0").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(1))).andExpect(
				jsonPath("$.content[*].name", contains("myjob.0"))).andExpect(
				jsonPath("$.content[*].unitName", contains("j3"))).andExpect(
				jsonPath("$.content[*].moduleType", contains("job"))).andExpect(
				jsonPath("$.content[*].containerId", contains("3"))).andExpect(
				jsonPath("$.content[*].moduleOptions.entity3", contains("value3"))).andExpect(
				jsonPath("$.content[*].deploymentProperties.criteria", contains("groups.contains('hdfs')")));
	}

	@Test
	public void testListModulesByNonExistingJobName() throws Exception {
		mockMvc.perform(get("/runtime/modules?jobname=notthere").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$", Matchers.hasSize(0)));
	}

	@Test
	public void testListModulesByContainer() throws Exception {
		mockMvc.perform(get("/runtime/modules?containerId=2").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(1))).andExpect(
				jsonPath("$.content[*].name", contains("log.1"))).andExpect(
				jsonPath("$.content[*].containerId", contains("2"))).andExpect(
				jsonPath("$.content[*].moduleType", contains("sink"))).andExpect(
				jsonPath("$.content[*].unitName", contains("s2"))).andExpect(
				jsonPath("$.content[*].deploymentStatus", contains("deployed"))).andExpect(
				jsonPath("$.content[*].moduleOptions.entity2", contains("value2"))).andExpect(
				jsonPath("$.content[*].deploymentProperties.count", contains("2")));
	}

	@Test
	public void testListModulesByContainerAndModuleId() throws Exception {
		mockMvc.perform(
				get("/runtime/modules?containerId=1&moduleId=s1.source.http.0").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$.name", containsString("http.0"))).andExpect(
				jsonPath("$.unitName", containsString("s1"))).andExpect(
				jsonPath("$.containerId", containsString("1"))).andExpect(
				jsonPath("$.moduleOptions.entity1", containsString("value1"))).andExpect(
				jsonPath("$.deploymentProperties.count", containsString("1")));
	}

	@Test
	public void testListNonExistingModule() throws Exception {
		mockMvc.perform(
				get("/runtime/modules?containerId=1&moduleId=random").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isNotFound()).andExpect(
				jsonPath("$[0].message",
						Matchers.is("The module with id 'random' doesn't exist in the container with id '1'")));
	}
}
