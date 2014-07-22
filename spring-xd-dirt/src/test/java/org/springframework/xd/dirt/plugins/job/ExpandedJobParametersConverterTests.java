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

package org.springframework.xd.dirt.plugins.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.junit.Test;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Gunnar Hillert
 */
public class ExpandedJobParametersConverterTests {

	@Test
	public void getJobParametersForNullProperties() throws Exception {

		final ExpandedJobParametersConverter jobParametersConverter = new ExpandedJobParametersConverter();

		final JobParameters jobParameters = jobParametersConverter.getJobParameters(null);
		assertNotNull(jobParameters);

		assertTrue(jobParameters.getParameters().size() == 1);
		assertNotNull(jobParameters.getString(ExpandedJobParametersConverter.UNIQUE_JOB_PARAMETER_KEY));

	}

	@Test
	public void getJobParametersForNullPropertiesWithMakeParametersUniqueExplicitlySetToTrue() throws Exception {

		final ExpandedJobParametersConverter jobParametersConverter = new ExpandedJobParametersConverter();
		jobParametersConverter.setMakeParametersUnique(true);

		final JobParameters jobParameters = jobParametersConverter.getJobParameters(null);
		assertNotNull(jobParameters);

		assertTrue(jobParameters.getParameters().size() == 1);
		assertNotNull(jobParameters.getString(ExpandedJobParametersConverter.UNIQUE_JOB_PARAMETER_KEY));

	}

	@Test
	public void getJobParametersForRestartWithMakeParametersUniqueExplicitlySetToTrue() throws Exception {

		final ExpandedJobParametersConverter jobParametersConverter = new ExpandedJobParametersConverter();
		jobParametersConverter.setMakeParametersUnique(true);

		final Properties properties = new Properties();
		properties.put(ExpandedJobParametersConverter.IS_RESTART_JOB_PARAMETER_KEY, String.valueOf(true));
		final JobParameters jobParameters = jobParametersConverter.getJobParameters(properties);
		assertNotNull(jobParameters);

		assertEquals(Integer.valueOf(1), Integer.valueOf(jobParameters.getParameters().size()));
		final JobParameters jobParametersWithoutRestartKey = jobParametersConverter.removeRestartParameterIfExists(jobParameters);
		assertEquals(Integer.valueOf(0), Integer.valueOf(jobParametersWithoutRestartKey.getParameters().size()));

	}

	@Test
	public void getJobParametersForEmptyProperties() throws Exception {

		final ExpandedJobParametersConverter jobParametersConverter = new ExpandedJobParametersConverter();

		final JobParameters jobParameters = jobParametersConverter.getJobParameters(new Properties());
		assertNotNull(jobParameters);

		assertTrue(jobParameters.getParameters().size() == 1);
		assertNotNull(jobParameters.getString(ExpandedJobParametersConverter.UNIQUE_JOB_PARAMETER_KEY));

	}

	@Test
	public void createJobParametersWithoutUniqueParam() throws Exception {

		final ExpandedJobParametersConverter jobParametersConverter = new ExpandedJobParametersConverter();
		jobParametersConverter.setMakeParametersUnique(false);

		final String json = "{\"param1\":\"Kenny\", \"param2\":\"Cartman\"}";

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

		final String json = "{\"param1\":\"Kenny\",\"param2\":\"Cartman\"}";

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

	@Test
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
		jobParametersConverter.setDateFormatAsString("yyyy/MM/dd");

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
	public void convertIdentifyingJobParametersToJsonWithIsRestartParamBeingAdded() throws Exception {

		final ExpandedJobParametersConverter jobParametersConverter = new ExpandedJobParametersConverter();
		jobParametersConverter.setMakeParametersUnique(true);

		final String jsonWithIsRestart = "{\"XD_isRestart\":\"true\",\"param1\":\"Kenny\",\"param2\":\"Cartman\"}";
		final String jsonWithoutIsRestart = "{\"param1\":\"Kenny\",\"param2\":\"Cartman\"}";

		final JobParameters jobParameters = jobParametersConverter.getJobParametersForJsonString(jsonWithIsRestart);

		assertNotNull(jobParameters);
		assertEquals(Integer.valueOf(3), Integer.valueOf(jobParameters.getParameters().size()));

		assertEquals("Kenny", jobParameters.getString("param1"));
		assertEquals("Cartman", jobParameters.getString("param2"));
		assertEquals("true", jobParameters.getString("XD_isRestart"));

		final JobParameters jobParametersWithoutRestart = jobParametersConverter.removeRestartParameterIfExists(jobParameters);
		assertEquals(Integer.valueOf(2), Integer.valueOf(jobParametersWithoutRestart.getParameters().size()));

		for (JobParameter jobParameter : jobParametersWithoutRestart.getParameters().values()) {
			assertTrue(jobParameter.isIdentifying());
		}

		final String jobParametersAsJsonWithIsRestart = jobParametersConverter.getJobParametersAsString(
				jobParametersWithoutRestart,
				true);
		assertEquals(jsonWithIsRestart, jobParametersAsJsonWithIsRestart);

		final String jobParametersAsJsonWithoutIsRestart = jobParametersConverter.getJobParametersAsString(
				jobParametersWithoutRestart, false);
		assertEquals(jsonWithoutIsRestart, jobParametersAsJsonWithoutIsRestart);
	}

	@Test
	public void verifyDefaultDateFormat() throws Exception {

		final SimpleDateFormat expectedDateFormat = new SimpleDateFormat("yyyy-MM-dd");

		final ExpandedJobParametersConverter jobParametersConverter = new ExpandedJobParametersConverter();

		final SimpleDateFormat actualDateFormat = TestUtils.getPropertyValue(jobParametersConverter, "dateFormat",
				SimpleDateFormat.class);

		assertEquals(expectedDateFormat.toPattern(), actualDateFormat.toPattern());

	}
}
