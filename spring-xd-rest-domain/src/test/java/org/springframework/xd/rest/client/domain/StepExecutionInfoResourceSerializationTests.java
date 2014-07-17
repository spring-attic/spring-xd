/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.rest.client.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.xd.rest.domain.StepExecutionInfoResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * 
 * @author Gunnar Hillert
 * @since 1.0
 */
public class StepExecutionInfoResourceSerializationTests {

	@Test
	public void testBasicSerializationOfStepExecutions() throws IOException {

		final ObjectMapper objectMapper = new ObjectMapper();

		final List<StepExecutionInfoResource> stepExecutionInfoResources = new ArrayList<>();

		final StepExecution stepExecution1 = new StepExecution("first step", new JobExecution(123L));
		final StepExecution stepExecution2 = new StepExecution("second step", new JobExecution(123L));

		final StepExecutionInfoResource executionInfoResource1 = new StepExecutionInfoResource(123L, stepExecution1, "stepType");
		final StepExecutionInfoResource executionInfoResource2 = new StepExecutionInfoResource(123L, stepExecution2, "stepType");

		stepExecutionInfoResources.add(executionInfoResource1);
		stepExecutionInfoResources.add(executionInfoResource2);

		final String stepExecutionInfoResourcesAsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
				stepExecutionInfoResources);
		Assert.assertNotNull(stepExecutionInfoResourcesAsJson);
	}
}
