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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
		ModuleMetadata entity1 = new ModuleMetadata("s1.source.http-0", "1", entityProps1, deploymentProps1);
		ModuleMetadata entity2 = new ModuleMetadata("s2.sink.log-1", "2", entityProps2, deploymentProps2);
		ModuleMetadata entity3 = new ModuleMetadata("s3.job.myjob-0", "3", entityProps3, deploymentProps3);
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
		when(moduleMetadataRepository.findOne("1", "s1.source.http-0")).thenReturn(entity1);
		when(moduleMetadataRepository.findAllByModuleId(pageable, "s3.job.myjob-0")).thenReturn(pageEntity3);
	}

	@Test
	public void testListModules() throws Exception {
		mockMvc.perform(get("/runtime/modules").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(3))).andExpect(
				jsonPath("$.content[*].moduleId", contains("s1.source.http-0", "s2.sink.log-1", "s3.job.myjob-0"))).andExpect(
				jsonPath("$.content[*].containerId", contains("1", "2", "3"))).andExpect(
				jsonPath("$.content[*].moduleOptions", Matchers.hasSize(3))).andExpect(
				jsonPath("$.content[*].deploymentProperties", Matchers.hasSize(3)));
	}

	@Test
	public void testListModulesByJobName() throws Exception {
		mockMvc.perform(get("/runtime/modules?jobname=s3").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$", Matchers.hasSize(1))).andExpect(
				jsonPath("$[*].moduleId", contains("s3.job.myjob-0"))).andExpect(
				jsonPath("$[*].containerId", contains("3"))).andExpect(
				jsonPath("$[*].moduleOptions.entity3", contains("value3"))).andExpect(
				jsonPath("$[*].deploymentProperties.criteria", contains("groups.contains('hdfs')")));
	}

	@Test
	public void testListModuleByModuleId() throws Exception {
		mockMvc.perform(get("/runtime/modules?moduleId=s3.job.myjob-0").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(1))).andExpect(
				jsonPath("$.content[*].moduleId", contains("s3.job.myjob-0"))).andExpect(
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
				jsonPath("$.content[*].moduleId", contains("s2.sink.log-1"))).andExpect(
				jsonPath("$.content[*].containerId", contains("2"))).andExpect(
				jsonPath("$.content[*].moduleOptions.entity2", contains("value2"))).andExpect(
				jsonPath("$.content[*].deploymentProperties.count", contains("2")));
	}

	@Test
	public void testListModulesByContainerAndModuleId() throws Exception {
		mockMvc.perform(
				get("/runtime/modules?containerId=1&moduleId=s1.source.http-0").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$.moduleId", containsString("s1.source.http-0"))).andExpect(
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
