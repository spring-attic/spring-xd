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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.xd.dirt.stream.NoSuchStreamException;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.StreamDeployer;

/**
 * Tests REST compliance of streams-related endpoints.
 * 
 * @author Eric Bottard
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, MockedDependencies.class })
public class StreamsControllerTest {

	@Autowired
	private StreamDeployer mockStreamDeployer;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Test
	public void testSuccessfulStreamCreation() throws Exception {
		when(mockStreamDeployer.deployStream("mystream", "http | hdfs")).thenReturn(
				new StreamDefinition("mystream", "http | hdfs"));

		mockMvc.perform(
				post("/streams").param("name", "mystream").param("definition", "http | hdfs")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
	}

	@Test
	public void testStreamCreationNoDefinition() throws Exception {
		mockMvc.perform(post("/streams").param("name", "mystream").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isBadRequest());
	}

	@Test
	public void testStreamCreationAnyError() throws Exception {
		doThrow(NullPointerException.class).when(mockStreamDeployer).deployStream(anyString(), anyString());

		mockMvc.perform(
				post("/streams").param("name", "mystream").param("definition", "file|http")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isInternalServerError());
	}

	@Test
	public void testSuccessfulStreamDeletion() throws Exception {
		mockMvc.perform(delete("/streams/{name}", "mystream")).andExpect(status().isOk());
		verify(mockStreamDeployer).undeployStream("mystream");
	}

	@Test
	public void testDeleteUnknownStream() throws Exception {
		when(mockStreamDeployer.undeployStream("mystream")).thenThrow(new NoSuchStreamException("mystream"));
		mockMvc.perform(delete("/streams/{name}", "mystream")).andExpect(status().isNotFound());
	}

	@Before
	public void setupMockMVC() {
		reset(mockStreamDeployer);
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
	}
}
