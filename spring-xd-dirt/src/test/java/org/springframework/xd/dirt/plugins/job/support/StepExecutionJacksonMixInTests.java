/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.plugins.job.support;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests that the {@link ExecutionContextJacksonMixIn} works as expected.
 *
 * @author Gunnar Hillert
 */
public class StepExecutionJacksonMixInTests {

	/**
	 * Assert that without using the {@link ExecutionContextJacksonMixIn} Jackson does not render the Step Execution
	 * Context correctly (Missing values).
	 *
	 * @throws JsonProcessingException
	 */
	@Test
	public void testSerializationOfSingleStepExecutionWithoutMixin() throws JsonProcessingException {

		final ObjectMapper objectMapper = new ObjectMapper();

		final StepExecution stepExecution = getStepExecution();
		final String result = objectMapper.writeValueAsString(stepExecution);

		assertThat(result, containsString("\"executionContext\":{\"dirty\":true,\"empty\":false}"));
	}

	/**
	 * Assert that by using the {@link ExecutionContextJacksonMixIn} Jackson renders the Step Execution Context
	 * correctly.
	 *
	 * @throws JsonProcessingException
	 */
	@Test
	public void testSerializationOfSingleStepExecution() throws JsonProcessingException {

		final ObjectMapper objectMapper = new ObjectMapper();

		objectMapper.addMixIn(StepExecution.class, StepExecutionJacksonMixIn.class);
		objectMapper.addMixIn(ExecutionContext.class, ExecutionContextJacksonMixIn.class);

		final StepExecution stepExecution = getStepExecution();
		final String result = objectMapper.writeValueAsString(stepExecution);

		assertThat(result, not(containsString("\"executionContext\":{\"dirty\":true,\"empty\":false}")));
		assertThat(result, containsString("\"executionContext\":{\"dirty\":true,\"empty\":false,\"values\":[{"));

		assertThat(result, containsString("{\"key\":\"counter\",\"value\":1234}"));
		assertThat(result, containsString("{\"key\":\"myDouble\",\"value\":1.123456}"));
		assertThat(result, containsString("{\"key\":\"Josh\",\"value\":4444444444}"));
		assertThat(result, containsString("{\"key\":\"awesomeString\",\"value\":\"Yep\"}"));
		assertThat(result, containsString("{\"key\":\"hello\",\"value\":\"world\""));
		assertThat(result, containsString("{\"key\":\"counter2\",\"value\":9999}"));
	}

	private StepExecution getStepExecution() {

		final StepExecution stepExecution = new StepExecution("step1", null);
		final ExecutionContext executionContext = stepExecution.getExecutionContext();

		executionContext.putInt("counter", 1234);
		executionContext.putDouble("myDouble", 1.123456d);
		executionContext.putLong("Josh", 4444444444L);
		executionContext.putString("awesomeString", "Yep");
		executionContext.put("hello", "world");
		executionContext.put("counter2", 9999);

		return stepExecution;
	}
}
