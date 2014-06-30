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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.junit.Test;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;

/**
 * @author Gunnar Hillert
 */
public class JobParametersBeanTests {

	@Test
	public void createJobParametersWithUniqueParam() throws Exception {

		final JobParametersBean jobParametersBean = new JobParametersBean(
				"{\"param1\":\"Kenny\", \"param2\":\"Cartman\"}");

		assertNull(jobParametersBean.getJobParameters());

		jobParametersBean.afterPropertiesSet();

		assertNotNull(jobParametersBean.getJobParameters());
		assertTrue(jobParametersBean.getJobParameters().getParameters().size() == 3);

		assertEquals("Kenny", jobParametersBean.getJobParameters().getString("param1"));
		assertEquals("Cartman", jobParametersBean.getJobParameters().getString("param2"));
		assertNotNull(jobParametersBean.getJobParameters().getString("random"));

	}

	@Test
	public void createJobParametersWithoutUniqueParam() throws Exception {

		final JobParametersBean jobParametersBean = new JobParametersBean(
				"{\"param1\":\"Kenny\", \"param2\":\"Cartman\"}");

		assertNull(jobParametersBean.getJobParameters());

		jobParametersBean.setMakeParametersUnique(false);
		jobParametersBean.afterPropertiesSet();

		final JobParameters jobParameters = jobParametersBean.getJobParameters();

		assertNotNull(jobParameters);
		assertTrue(jobParameters.getParameters().size() == 2);

		assertEquals("Kenny", jobParameters.getString("param1"));
		assertEquals("Cartman", jobParameters.getString("param2"));

		for (JobParameter jobParameter : jobParameters.getParameters().values()) {
			assertTrue(jobParameter.isIdentifying());
		}
	}

	@Test
	public void createNotIdentifyingJobParameters() throws Exception {

		final JobParametersBean jobParametersBean = new JobParametersBean(
				"{\"-param1\":\"Kenny\", \"-param2\":\"Cartman\"}");

		assertNull(jobParametersBean.getJobParameters());

		jobParametersBean.setMakeParametersUnique(false);
		jobParametersBean.afterPropertiesSet();

		final JobParameters jobParameters = jobParametersBean.getJobParameters();
		assertNotNull(jobParameters);

		assertTrue(jobParameters.getParameters().size() == 2);

		assertEquals("Kenny", jobParameters.getString("param1"));
		assertEquals("Cartman", jobParameters.getString("param2"));

		for (JobParameter jobParameter : jobParameters.getParameters().values()) {
			assertFalse(jobParameter.isIdentifying());
		}
	}

	@Test
	public void createTypedJobParametersWithDefaultDateFormat() throws Exception {

		final JobParametersBean jobParametersBean = new JobParametersBean(
				"{\"param1(long)\":\"1234\", \"mydate(date)\":\"1978-05-01\"}");

		assertNull(jobParametersBean.getJobParameters());

		jobParametersBean.setMakeParametersUnique(false);
		jobParametersBean.afterPropertiesSet();

		final JobParameters jobParameters = jobParametersBean.getJobParameters();
		assertNotNull(jobParameters);

		assertTrue(jobParameters.getParameters().size() == 2);

		assertEquals("1234", jobParameters.getString("param1"));
		assertEquals(Long.valueOf(1234), jobParameters.getLong("param1"));

		final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		assertEquals(dateFormat.parse("1978/05/01"), jobParameters.getDate("mydate"));

	}

	@Test
	public void createTypedJobParametersWithCustomDateFormat() throws Exception {

		final JobParametersBean jobParametersBean = new JobParametersBean(
				"{\"param1(long)\":\"1234\", \"mydate(date)\":\"1978/05/01\"}");

		assertNull(jobParametersBean.getJobParameters());

		jobParametersBean.setMakeParametersUnique(false);
		jobParametersBean.setDateFormatAsString("yyyy/MM/dd");
		jobParametersBean.afterPropertiesSet();

		final JobParameters jobParameters = jobParametersBean.getJobParameters();
		assertNotNull(jobParameters);

		assertTrue(jobParameters.getParameters().size() == 2);

		assertEquals("1234", jobParameters.getString("param1"));
		assertEquals(Long.valueOf(1234), jobParameters.getLong("param1"));

		final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		assertEquals(dateFormat.parse("1978/05/01"), jobParameters.getDate("mydate"));

	}

	@Test
	public void createTypedJobParametersWithCustomDateFormat2() throws Exception {

		final JobParametersBean jobParametersBean = new JobParametersBean(
				"{\"param1\":\"We should all use ISO dates\", \"mydate(date)\":\"2013-08-15T14:50Z\"}");

		assertNull(jobParametersBean.getJobParameters());

		final DateFormat isoDateformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		jobParametersBean.setDateFormat(isoDateformat);
		jobParametersBean.afterPropertiesSet();

		final JobParameters jobParameters = jobParametersBean.getJobParameters();
		assertNotNull(jobParameters);

		assertTrue(jobParameters.getParameters().size() == 3);

		assertEquals(isoDateformat.parse("2013-08-15T14:50Z"), jobParameters.getDate("mydate"));
	}

	@Test
	public void createTypedJobParametersWithNullDateFormat() throws Exception {
		final JobParametersBean jobParametersBean = new JobParametersBean(
				"{\"param1\":\"We should all use ISO dates\", \"mydate(date)\":\"2013-08-15T14:50Z\"}");
		assertNull(jobParametersBean.getJobParameters());

		try {
			jobParametersBean.setDateFormat(null);
		}
		catch (IllegalArgumentException e) {
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void createTypedJobParametersWithNullNumberFormat() throws Exception {
		final JobParametersBean jobParametersBean = new JobParametersBean("{\"param1(long)\":\"123\"}");
		assertNull(jobParametersBean.getJobParameters());

		try {
			jobParametersBean.setNumberFormat(null);
		}
		catch (IllegalArgumentException e) {
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void createTypedJobParametersWithCustomNumberFormat() throws Exception {
		final JobParametersBean jobParametersBean = new JobParametersBean("{\"param1(double)\":\"123,55\"}");
		assertNull(jobParametersBean.getJobParameters());

		final NumberFormat germanNumberFormat = NumberFormat.getInstance(Locale.GERMANY);
		jobParametersBean.setNumberFormat(germanNumberFormat);
		jobParametersBean.afterPropertiesSet();

		final JobParameters jobParameters = jobParametersBean.getJobParameters();
		assertNotNull(jobParameters);

		assertTrue(jobParameters.getParameters().size() == 2);

		assertEquals(Double.valueOf(123.55), jobParameters.getDouble("param1"));
	}

	@Test
	public void createEmptyJobParametersWithUniqueParameter() throws Exception {
		final JobParametersBean jobParametersBean = new JobParametersBean(null);
		assertNull(jobParametersBean.getJobParameters());

		jobParametersBean.afterPropertiesSet();

		final JobParameters jobParameters = jobParametersBean.getJobParameters();
		assertNotNull(jobParameters);

		assertTrue(jobParameters.getParameters().size() == 1);
	}

	@Test
	public void createEmptyJobParametersWithOutUniqueParameter() throws Exception {
		final JobParametersBean jobParametersBean = new JobParametersBean(null);
		assertNull(jobParametersBean.getJobParameters());

		jobParametersBean.setMakeParametersUnique(false);
		jobParametersBean.afterPropertiesSet();

		final JobParameters jobParameters = jobParametersBean.getJobParameters();
		assertNotNull(jobParameters);

		assertTrue(jobParameters.getParameters().size() == 0);
	}

	@Test
	public void createJobParametersWithUniqueParameterAndEmptyJSONString() throws Exception {
		final JobParametersBean jobParametersBean = new JobParametersBean("");
		assertNull(jobParametersBean.getJobParameters());

		jobParametersBean.afterPropertiesSet();

		final JobParameters jobParameters = jobParametersBean.getJobParameters();
		assertNotNull(jobParameters);

		assertTrue(jobParameters.getParameters().size() == 1);
	}

	@Test
	public void createJobParametersWithParamThatMatchesRandom() throws Exception {
		final JobParametersBean jobParametersBean = new JobParametersBean("{\"random\":\"Will throw Exception\"}");

		assertNull(jobParametersBean.getJobParameters());

		try {
			jobParametersBean.afterPropertiesSet();
		}
		catch (IllegalStateException e) {
			return;
		}

		fail("Expected an IllegalStateException to be thrown.");
	}

	@Test
	public void createJobParametersWithInvalidJSON() throws Exception {
		final JobParametersBean jobParametersBean = new JobParametersBean("this should fail");

		assertNull(jobParametersBean.getJobParameters());

		try {
			jobParametersBean.afterPropertiesSet();
		}
		catch (IllegalArgumentException e) {
			assertEquals("Unable to convert provided JSON to Map<String, Object>", e.getMessage());
			return;
		}

		fail("Expected an IllegalStateException to be thrown.");
	}

	@Test
	public void createJobParametersWithUTFString() throws Exception {

		/** I want to go to Japan. */
		final String stringToPostInJapanese = "\u65e5\u672c\u306b\u884c\u304d\u305f\u3044\u3002";

		final JobParametersBean jobParametersBean = new JobParametersBean("{\"param1\":\"" + stringToPostInJapanese
				+ "\"}");

		assertNull(jobParametersBean.getJobParameters());
		jobParametersBean.afterPropertiesSet();

		assertNotNull(jobParametersBean.getJobParameters());
		assertTrue(jobParametersBean.getJobParameters().getParameters().size() == 2);
		assertEquals(stringToPostInJapanese, jobParametersBean.getJobParameters().getString("param1"));
	}
}
