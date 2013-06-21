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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.xd.dirt.stream.StreamDeployer;

/**
 * Tests REST compliance of streams-related endpoints.
 * 
 * @author Eric Bottard
 */
@RunWith(MockitoJUnitRunner.class)
public class StreamsControllerTest {

	@Mock
	private StreamDeployer mockStreamDeployer;

	private MockMvc mockMvc;

	@Test
	public void testSuccessfulStreamCreation() throws Exception {
		mockMvc.perform(
				put("/streams/{name}", "mystream").content("http | hdfs"))
				.andExpect(status().isCreated());
		verify(mockStreamDeployer).deployStream("mystream", "http | hdfs");
	}

	@Test
	public void testStreamCreationEmptyBody() throws Exception {
		mockMvc.perform(put("/streams/{name}", "mystream")).andExpect(
				status().isBadRequest());
	}

	@Test
	public void testStreamCreationAnyError() throws Exception {
		doThrow(NullPointerException.class).when(mockStreamDeployer)
				.deployStream(anyString(), anyString());

		mockMvc.perform(
				put("/streams/{name}", "mystream").content("doesn't matter"))
				.andExpect(status().isInternalServerError());
	}

	@Test
	public void testSuccessfulStreamDeletion() throws Exception {
		mockMvc.perform(delete("/streams/{name}", "mystream")).andExpect(
				status().isOk());
		verify(mockStreamDeployer).undeployStream("mystream");
	}

	@Before
	public void setupMockMVC() {
		this.mockMvc = MockMvcBuilders.standaloneSetup(
				new StreamsController(mockStreamDeployer)).build();
	}

}
