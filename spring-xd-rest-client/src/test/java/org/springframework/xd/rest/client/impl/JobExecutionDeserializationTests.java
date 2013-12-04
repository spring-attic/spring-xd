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

package org.springframework.xd.rest.client.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.xd.rest.client.domain.JobExecutionInfoResource;
import org.springframework.xd.rest.client.impl.support.ExecutionContextJacksonMixIn;
import org.springframework.xd.rest.client.impl.support.ExitStatusJacksonMixIn;
import org.springframework.xd.rest.client.impl.support.JobExecutionJacksonMixIn;
import org.springframework.xd.rest.client.impl.support.JobInstanceJacksonMixIn;
import org.springframework.xd.rest.client.impl.support.JobParameterJacksonMixIn;
import org.springframework.xd.rest.client.impl.support.JobParametersJacksonMixIn;
import org.springframework.xd.rest.client.impl.support.StepExecutionJacksonMixIn;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * @author Gunnar Hillert
 */
public class JobExecutionDeserializationTests {

	@Test
	public void test() throws IOException {

		final ObjectMapper objectMapper = new ObjectMapper();

		final InputStream inputStream = JobExecutionDeserializationTests.class.getResourceAsStream("/JobExecutionJson.txt");

		final String json = IOUtils.toString(inputStream);

		objectMapper.addMixInAnnotations(JobExecution.class, JobExecutionJacksonMixIn.class);
		objectMapper.addMixInAnnotations(JobParameters.class, JobParametersJacksonMixIn.class);
		objectMapper.addMixInAnnotations(JobInstance.class, JobInstanceJacksonMixIn.class);
		objectMapper.addMixInAnnotations(StepExecution.class, StepExecutionJacksonMixIn.class);
		objectMapper.addMixInAnnotations(ExecutionContext.class, ExecutionContextJacksonMixIn.class);
		objectMapper.addMixInAnnotations(ExitStatus.class, ExitStatusJacksonMixIn.class);

		final JobExecutionInfoResource[] jobExecutionInfoResources = objectMapper.readValue(json,
				JobExecutionInfoResource[].class);

		Assert.assertTrue("Expect 1 JobExecutionInfoResource", jobExecutionInfoResources.length == 1);

		final JobExecutionInfoResource jobExecutionInfoResource = jobExecutionInfoResources[0];

		Assert.assertEquals(Long.valueOf(0), jobExecutionInfoResource.getJobId());
		Assert.assertEquals("mm", jobExecutionInfoResource.getName());
		Assert.assertEquals("COMPLETED", jobExecutionInfoResource.getJobExecution().getStatus().name());

	}

	@Test
	public void testDeserializationOfSingleJobExecution() throws IOException {

		final ObjectMapper objectMapper = new ObjectMapper();

		final InputStream inputStream = JobExecutionDeserializationTests.class.getResourceAsStream("/SingleJobExecutionJson.txt");

		final String json = IOUtils.toString(inputStream);

		objectMapper.addMixInAnnotations(JobExecution.class, JobExecutionJacksonMixIn.class);
		objectMapper.addMixInAnnotations(JobParameters.class, JobParametersJacksonMixIn.class);
		objectMapper.addMixInAnnotations(JobParameter.class, JobParameterJacksonMixIn.class);
		objectMapper.addMixInAnnotations(JobInstance.class, JobInstanceJacksonMixIn.class);
		objectMapper.addMixInAnnotations(StepExecution.class, StepExecutionJacksonMixIn.class);
		objectMapper.addMixInAnnotations(ExecutionContext.class, ExecutionContextJacksonMixIn.class);
		objectMapper.addMixInAnnotations(ExitStatus.class, ExitStatusJacksonMixIn.class);

		final JobExecutionInfoResource jobExecutionInfoResource = objectMapper.readValue(json,
				JobExecutionInfoResource.class);

		Assert.assertNotNull(jobExecutionInfoResource);
		Assert.assertEquals(Long.valueOf(0), jobExecutionInfoResource.getJobId());
		Assert.assertEquals("ff", jobExecutionInfoResource.getName());
		Assert.assertEquals("COMPLETED", jobExecutionInfoResource.getJobExecution().getStatus().name());

	}

}
