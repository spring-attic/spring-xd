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

package org.springframework.xd.dirt.plugins.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;

/**
 * @author Gunnar Hillert
 */
public class ExpandedJobParametersConverterTests {

	@Test
	public void getJobParametersForNullProperties() throws Exception {

		final ExpandedJobParametersConverter jobParametersConverter = new ExpandedJobParametersConverter();

		JobParameters jobParameters = jobParametersConverter.getJobParameters(null);
		assertNotNull(jobParameters);

		assertTrue(jobParameters.getParameters().size() == 1);
		assertNotNull(jobParameters.getString(ExpandedJobParametersConverter.UNIQUE_JOB_PARAMETER_KEY));

	}

	@Test
	public void getJobParametersForEmptyProperties() throws Exception {

		final ExpandedJobParametersConverter jobParametersConverter = new ExpandedJobParametersConverter();

		JobParameters jobParameters = jobParametersConverter.getJobParameters(new Properties());
		assertNotNull(jobParameters);

		assertTrue(jobParameters.getParameters().size() == 1);
		assertNotNull(jobParameters.getString(ExpandedJobParametersConverter.UNIQUE_JOB_PARAMETER_KEY));

	}

	@Test
	public void createJobParametersWithoutUniqueParam() throws Exception {

		final ExpandedJobParametersConverter jobParametersConverter = new ExpandedJobParametersConverter();
		jobParametersConverter.setMakeParametersUnique(false);

		String json = "{\"param1\":\"Kenny\", \"param2\":\"Cartman\"}";

		final JobParameters jobParameters = jobParametersConverter.getJobParametersForJsonString(json);

		assertNotNull(jobParameters);
		assertTrue(jobParameters.getParameters().size() == 2);

		assertEquals("Kenny", jobParameters.getString("param1"));
		assertEquals("Cartman", jobParameters.getString("param2"));

		for (JobParameter jobParameter : jobParameters.getParameters().values()) {
			assertTrue(jobParameter.isIdentifying());
		}

	}

	@Test
	public void convertIdentifyingJobParametersToJson() throws Exception {

		final ExpandedJobParametersConverter jobParametersConverter = new ExpandedJobParametersConverter();
		jobParametersConverter.setMakeParametersUnique(false);

		String json = "{\"param1\":\"Kenny\",\"param2\":\"Cartman\"}";

		final JobParameters jobParameters = jobParametersConverter.getJobParametersForJsonString(json);

		assertNotNull(jobParameters);
		assertTrue(jobParameters.getParameters().size() == 2);

		assertEquals("Kenny", jobParameters.getString("param1"));
		assertEquals("Cartman", jobParameters.getString("param2"));

		for (JobParameter jobParameter : jobParameters.getParameters().values()) {
			assertTrue(jobParameter.isIdentifying());
		}

		final String jobParametersAsJson = jobParametersConverter.getJobParametersAsString(jobParameters);

		assertEquals(json, jobParametersAsJson);

	}

	/**
	 * TODO See https://jira.springsource.org/browse/BATCH-2179
	 */
	@Test
	@Ignore
	public void convertNonIdentifyingJobParametersToJson() throws Exception {

		final ExpandedJobParametersConverter jobParametersConverter = new ExpandedJobParametersConverter();
		jobParametersConverter.setMakeParametersUnique(false);

		final String json = "{\"-param1\":\"Kenny\",\"-param2\":\"Cartman\"}";

		final JobParameters jobParameters = jobParametersConverter.getJobParametersForJsonString(json);

		assertNotNull(jobParameters);
		assertTrue(jobParameters.getParameters().size() == 2);

		assertEquals("Kenny", jobParameters.getString("param1"));
		assertEquals("Cartman", jobParameters.getString("param2"));

		for (JobParameter jobParameter : jobParameters.getParameters().values()) {
			assertFalse(jobParameter.isIdentifying());
		}

		final String jobParametersAsJson = jobParametersConverter.getJobParametersAsString(jobParameters);

		assertEquals(json, jobParametersAsJson);

	}

	@Test
	public void convertDateAndNumberJobParametersToJson() throws Exception {

		final ExpandedJobParametersConverter jobParametersConverter = new ExpandedJobParametersConverter();
		jobParametersConverter.setMakeParametersUnique(false);
		final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");

		final Date date = simpleDateFormat.parse("2014/02/18");

		final String json = "{\"param1(date)\":\"2014/02/18\",\"param2(long)\":\"123456789\"}";

		final JobParameters jobParameters = jobParametersConverter.getJobParametersForJsonString(json);

		assertNotNull(jobParameters);
		assertTrue(jobParameters.getParameters().size() == 2);

		assertEquals(date, jobParameters.getDate("param1"));
		assertEquals(Long.valueOf(123456789L), jobParameters.getLong("param2"));

		for (JobParameter jobParameter : jobParameters.getParameters().values()) {
			assertTrue(jobParameter.isIdentifying());
		}

		final String jobParametersAsJson = jobParametersConverter.getJobParametersAsString(jobParameters);

		assertEquals(json, jobParametersAsJson);

	}

	@Test
	public void convertNullJobParametersToJson() throws Exception {

		final ExpandedJobParametersConverter jobParametersConverter = new ExpandedJobParametersConverter();

		try {
			jobParametersConverter.getJobParametersAsString(null, true);
		}
		catch (IllegalArgumentException e) {
			assertEquals("jobParameters must not be null.", e.getMessage());
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void convertIdentifyingJobParametersToJsonWithRandomParamBeingRemoved() throws Exception {

		final ExpandedJobParametersConverter jobParametersConverter = new ExpandedJobParametersConverter();
		jobParametersConverter.setMakeParametersUnique(true);

		String json = "{\"param1\":\"Kenny\",\"param2\":\"Cartman\"}";

		final JobParameters jobParameters = jobParametersConverter.getJobParametersForJsonString(json);

		assertNotNull(jobParameters);
		assertTrue(jobParameters.getParameters().size() == 3);

		assertEquals("Kenny", jobParameters.getString("param1"));
		assertEquals("Cartman", jobParameters.getString("param2"));
		assertNotNull(jobParameters.getString("random"));

		for (JobParameter jobParameter : jobParameters.getParameters().values()) {
			assertTrue(jobParameter.isIdentifying());
		}

		final String jobParametersAsJson = jobParametersConverter.getJobParametersAsString(jobParameters, true);

		assertEquals(json, jobParametersAsJson);

	}
}
