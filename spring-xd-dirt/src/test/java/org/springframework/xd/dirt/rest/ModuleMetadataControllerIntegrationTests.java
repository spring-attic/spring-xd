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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.dirt.module.store.ModuleMetadata;

/**
 * Tests REST compliance of module metadata endpoint.
 *
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, Dependencies.class })
public class ModuleMetadataControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Before
	public void before() {
		PageRequest pageable = new PageRequest(0, 20);
		ModuleMetadata entity1 = new ModuleMetadata("s1.source.http-0", "1", "{entity1: value1}", "{count:1}");
		ModuleMetadata entity2 = new ModuleMetadata("s2.sink.log-1", "2", "{entity2: value2}", "{count:2}");
		ModuleMetadata entity3 = new ModuleMetadata("s3.job.myjob-0", "3", "{entity3: value3}",
				"{criteria:groups.contains('hdfs')}");
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
		Page<ModuleMetadata> pagedEntity1 = new PageImpl<>(entities1);
		Page<ModuleMetadata> pagedEntity2 = new PageImpl<>(entities2);
		Page<ModuleMetadata> pagedEntity3 = new PageImpl<>(entities3);
		Page<ModuleMetadata> allPages = new PageImpl<>(all);
		when(moduleMetadataRepository.findAll(pageable)).thenReturn(allPages);
		when(moduleMetadataRepository.findAll()).thenReturn(all);
		when(moduleMetadataRepository.findAllByContainerId("2")).thenReturn(pagedEntity2);
		when(moduleMetadataRepository.findAllByContainerAndModuleId("1", "s1.source.http-0")).thenReturn(pagedEntity1);
		when(moduleMetadataRepository.findAllByModuleId("s3.job.myjob-0")).thenReturn(pagedEntity3);
	}

	@Test
	public void testListModules() throws Exception {
		mockMvc.perform(get("/runtime/modules").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(3))).andExpect(
				jsonPath("$.content[*].moduleId", contains("s1.source.http-0", "s2.sink.log-1", "s3.job.myjob-0"))).andExpect(
				jsonPath("$.content[*].containerId", contains("1", "2", "3"))).andExpect(
				jsonPath("$.content[*].moduleProperties",
						contains("{entity1: value1}", "{entity2: value2}", "{entity3: value3}"))).andExpect(
				jsonPath("$.content[*].deploymentProperties",
						contains("{count:1}", "{count:2}", "{criteria:groups.contains('hdfs')}")));
	}

	@Test
	public void testListModulesByJobName() throws Exception {
		mockMvc.perform(get("/runtime/modules?jobname=s3").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$", Matchers.hasSize(1))).andExpect(
				jsonPath("$[*].moduleId", contains("s3.job.myjob-0"))).andExpect(
				jsonPath("$[*].containerId", contains("3"))).andExpect(
				jsonPath("$[*].moduleProperties", contains("{entity3: value3}"))).andExpect(
				jsonPath("$[*].deploymentProperties", contains("{criteria:groups.contains('hdfs')}")));
	}

	@Test
	public void testListModuleByModuleId() throws Exception {
		mockMvc.perform(get("/runtime/modules?moduleId=s3.job.myjob-0").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(1))).andExpect(
				jsonPath("$content[*].moduleId", contains("s3.job.myjob-0"))).andExpect(
				jsonPath("$content[*].containerId", contains("3"))).andExpect(
				jsonPath("$content[*].moduleProperties", contains("{entity3: value3}"))).andExpect(
				jsonPath("$content[*].deploymentProperties", contains("{criteria:groups.contains('hdfs')}")));
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
				jsonPath("$.content[*].moduleId", contains("s2.sink.log-1"))).andExpect(
				jsonPath("$.content[*].containerId", contains("2"))).andExpect(
				jsonPath("$.content[*].moduleProperties", contains("{entity2: value2}"))).andExpect(
				jsonPath("$.content[*].deploymentProperties", contains("{count:2}")));
	}

	@Test
	public void testListModulesByContainerAndModuleId() throws Exception {
		mockMvc.perform(
				get("/runtime/modules?containerId=1&moduleId=s1.source.http-0").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(1))).andExpect(
				jsonPath("$.content[*].moduleId", contains("s1.source.http-0"))).andExpect(
				jsonPath("$.content[*].containerId", contains("1"))).andExpect(
				jsonPath("$.content[*].moduleProperties", contains("{entity1: value1}"))).andExpect(
				jsonPath("$.content[*].deploymentProperties", contains("{count:1}")));
	}
}
