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
import org.springframework.xd.dirt.module.store.RuntimeModuleInfoEntity;

/**
 * Tests REST compliance of runtime modules endpoint.
 * 
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, Dependencies.class })
public class RuntimeModulesControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Before
	public void before() {
		PageRequest pageable = new PageRequest(0, 20);
		RuntimeModuleInfoEntity entity1 = new RuntimeModuleInfoEntity("1", "foo", "0", "{}");
		RuntimeModuleInfoEntity entity2 = new RuntimeModuleInfoEntity("2", "bar", "1", "{}");
		List<RuntimeModuleInfoEntity> entities1 = new ArrayList<RuntimeModuleInfoEntity>();
		List<RuntimeModuleInfoEntity> entities2 = new ArrayList<RuntimeModuleInfoEntity>();
		List<RuntimeModuleInfoEntity> entities3 = new ArrayList<RuntimeModuleInfoEntity>();
		entities1.add(entity1);
		entities1.add(entity2);
		entities2.add(entity1);
		entities3.add(entity2);
		Page<RuntimeModuleInfoEntity> pagedEntity1 = new PageImpl<>(entities1);
		Page<RuntimeModuleInfoEntity> pagedEntity2 = new PageImpl<>(entities2);
		Page<RuntimeModuleInfoEntity> pagedEntity3 = new PageImpl<>(entities3);
		when(modulesRepository.findAll(pageable)).thenReturn(pagedEntity1);
		when(containerModulesRepository.findAllByContainerId(pageable, "1")).thenReturn(pagedEntity2);
		when(containerModulesRepository.findAllByContainerId(pageable, "2")).thenReturn(pagedEntity3);
	}

	@Test
	public void testListModules() throws Exception {
		mockMvc.perform(get("/runtime/modules").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(2))).andExpect(
				jsonPath("$.content[*].containerId", contains("1", "2"))).andExpect(
				jsonPath("$.content[*].group", contains("foo", "bar"))).andExpect(
				jsonPath("$.content[*].index", contains("0", "1")));
	}

	@Test
	public void testListModulesByContainer() throws Exception {
		mockMvc.perform(get("/runtime/modules?containerId=1").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(1))).andExpect(
				jsonPath("$.content[*].containerId", contains("1"))).andExpect(
				jsonPath("$.content[*].group", contains("foo"))).andExpect(
				jsonPath("$.content[*].index", contains("0")));
	}
}
