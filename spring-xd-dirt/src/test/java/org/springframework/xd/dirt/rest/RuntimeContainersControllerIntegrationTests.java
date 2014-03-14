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
import org.springframework.xd.dirt.container.ContainerMetadata;

/**
 * Tests REST compliance of containers endpoint.
 * 
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, Dependencies.class })
public class RuntimeContainersControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Before
	public void before() {
		PageRequest pageable = new PageRequest(0, 20);
		ContainerMetadata container1 = new ContainerMetadata("1", 1234, "host1", "127.0.0.1");
		ContainerMetadata container2 = new ContainerMetadata("2", 2345, "host2", "192.168.2.1");
		List<ContainerMetadata> containerEntities = new ArrayList<ContainerMetadata>();
		containerEntities.add(container1);
		containerEntities.add(container2);
		Page<ContainerMetadata> pagedEntity = new PageImpl<ContainerMetadata>(containerEntities);
		when(containerMetadataRepository.findAll(pageable)).thenReturn(pagedEntity);
	}

	@Test
	public void testListModules() throws Exception {
		mockMvc.perform(get("/runtime/containers").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(2))).andExpect(
				jsonPath("$.content[*].containerId", contains("1", "2"))).andExpect(
				jsonPath("$.content[*].processId", contains(1234, 2345))).andExpect(
				jsonPath("$.content[*].hostName", contains("host1", "host2"))).andExpect(
				jsonPath("$.content[*].ipAddress", contains("127.0.0.1", "192.168.2.1")));
	}
}
