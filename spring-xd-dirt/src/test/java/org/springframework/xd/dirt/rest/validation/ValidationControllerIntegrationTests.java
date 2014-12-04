/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.rest.validation;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.dirt.rest.AbstractControllerIntegrationTest;
import org.springframework.xd.dirt.rest.Dependencies;
import org.springframework.xd.dirt.rest.RestConfiguration;
import org.springframework.xd.rest.domain.validation.CronValidation;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests REST compliance of validation endpoint.
 *
 * @author Gunnar Hillert
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, Dependencies.class })
public class ValidationControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Test
	public void testValidCronExpression() throws Exception {
		final CronValidation cronValidation = new CronValidation();
		cronValidation.setCronExpression("1 1 1 1 1 1");
		mockMvc.perform(
				post("/validation/cron").content(new ObjectMapper().writeValueAsString(cronValidation)).accept(
						MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(
				jsonPath("$.cronExpression", equalToIgnoringCase("1 1 1 1 1 1"))).andExpect(
				jsonPath("$.valid", equalTo(true))).andExpect(
				jsonPath("$.errorMessage", nullValue())).andExpect(
				jsonPath("$.nextExecutionTime", notNullValue()));
	}

	@Test
	public void testInValidCronExpression() throws Exception {
		final CronValidation cronValidation = new CronValidation();
		cronValidation.setCronExpression("1 1");
		mockMvc.perform(
				post("/validation/cron").content(new ObjectMapper().writeValueAsString(cronValidation)).accept(
						MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(
				jsonPath("$.cronExpression", equalToIgnoringCase("1 1"))).andExpect(
				jsonPath("$.valid", equalTo(false))).andExpect(
				jsonPath("$.errorMessage",
						equalToIgnoringCase("Cron expression must consist of 6 fields (found 2 in \"1 1\")"))).andExpect(
				jsonPath("$.nextExecutionTime", nullValue()));
	}

	@Test
	public void testEmptyCronExpression() throws Exception {
		final CronValidation cronValidation = new CronValidation();
		cronValidation.setCronExpression("");
		mockMvc.perform(
				post("/validation/cron").content(new ObjectMapper().writeValueAsString(cronValidation)).accept(
						MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(
				jsonPath("$.cronExpression", isEmptyString())).andExpect(
				jsonPath("$.valid", equalTo(false))).andExpect(
				jsonPath("$.errorMessage",
						equalToIgnoringCase("The cron expression must not be empty."))).andExpect(
				jsonPath("$.nextExecutionTime", nullValue()));
	}
}
