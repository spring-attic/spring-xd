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
import static org.mockito.Mockito.when;

import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
public class JobLaunchRequestTransformerTests {

	JobRegistry mockedJobRegistry;

	Job mockedJob;

	JobLaunchRequestTransformer transformer;

	RunIdIncrementer jobParameterIncrementer = new RunIdIncrementer();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Before
	public void setup() throws Exception {
		mockedJob = Mockito.mock(Job.class);
		mockedJobRegistry = Mockito.mock(JobRegistry.class);
		when(mockedJobRegistry.getJob("testJob")).thenReturn(mockedJob);
		transformer = new JobLaunchRequestTransformer(mockedJobRegistry, "testJob");
	}

	@Test
	public void createJobParametersWithUniqueParam() throws Exception {

		final Message<String> message = MessageBuilder.withPayload("{\"param1\":\"Kenny\", \"param2\":\"Cartman\"}").build();

		final JobLaunchRequest jobLaunchRequest = transformer.toJobLaunchRequest(message);

		assertNotNull(jobLaunchRequest.getJobParameters());
		assertTrue(jobLaunchRequest.getJobParameters().getParameters().size() == 3);

		assertEquals("Kenny", jobLaunchRequest.getJobParameters().getString("param1"));
		assertEquals("Cartman", jobLaunchRequest.getJobParameters().getString("param2"));
		assertNotNull(jobLaunchRequest.getJobParameters().getString("random"));

	}

	@Test
	public void createJobParametersUsingPropertiesWithUniqueParam() throws Exception {

		final Properties properties = new Properties();
		properties.put("param1", "Kenny");
		properties.put("param2", "Cartman");

		final Message<Properties> message = MessageBuilder.withPayload(properties).build();

		final JobLaunchRequest jobLaunchRequest = transformer.toJobLaunchRequest(message);

		assertNotNull(jobLaunchRequest.getJobParameters());
		assertTrue(jobLaunchRequest.getJobParameters().getParameters().size() == 3);

		assertEquals("Kenny", jobLaunchRequest.getJobParameters().getString("param1"));
		assertEquals("Cartman", jobLaunchRequest.getJobParameters().getString("param2"));
		assertNotNull(jobLaunchRequest.getJobParameters().getString("random"));

	}

	@Test
	public void createJobParametersUsingMapWithUniqueParam() throws Exception {

		final Map<String, String> map = new HashMap<String, String>();
		map.put("param1", "Kenny");
		map.put("param2", "Cartman");

		final Message<Map<String, String>> message = MessageBuilder.withPayload(map).build();

		final JobLaunchRequest jobLaunchRequest = transformer.toJobLaunchRequest(message);

		assertNotNull(jobLaunchRequest.getJobParameters());
		assertTrue(jobLaunchRequest.getJobParameters().getParameters().size() == 3);

		assertEquals("Kenny", jobLaunchRequest.getJobParameters().getString("param1"));
		assertEquals("Cartman", jobLaunchRequest.getJobParameters().getString("param2"));
		assertNotNull(jobLaunchRequest.getJobParameters().getString("random"));

	}

	@Test
	public void createJobParametersUsingFileWithUniqueParam() throws Exception {

		final File file = temporaryFolder.newFile("MyPayloadFile.txt");
		final Message<File> message = MessageBuilder.withPayload(file).build();

		final JobLaunchRequest jobLaunchRequest = transformer.toJobLaunchRequest(message);
		final JobParameters jobParameters = jobLaunchRequest.getJobParameters();

		assertNotNull(jobParameters);
		assertTrue(jobParameters.getParameters().size() == 2);

		assertTrue(
				String.format("Property '%s' did not end with '%s'.",
						ExpandedJobParametersConverter.ABSOLUTE_FILE_PATH, "MyPayloadFile.txt"),
						jobParameters.getString(ExpandedJobParametersConverter.ABSOLUTE_FILE_PATH).endsWith(
								"MyPayloadFile.txt"));
		assertNotNull(jobLaunchRequest.getJobParameters().getString("random"));

	}

	@Test
	public void createJobParametersUsingTupleWithUniqueParam() throws Exception {

		final Tuple tuple = TupleBuilder.tuple().of("foo", "123,456");
		final Message<Tuple> message = MessageBuilder.withPayload(tuple).build();

		final JobLaunchRequest jobLaunchRequest = transformer.toJobLaunchRequest(message);
		final JobParameters jobParameters = jobLaunchRequest.getJobParameters();

		assertNotNull(jobParameters);
		assertTrue(jobParameters.getParameters().size() == 2);

		assertEquals("123,456", jobParameters.getString("foo"));
		assertNotNull(jobLaunchRequest.getJobParameters().getString("random"));
	}

	@Test
	public void createJobParametersWithoutUniqueParam() throws Exception {

		final Message<String> message = MessageBuilder.withPayload("{\"param1\":\"Kenny\", \"param2\":\"Cartman\"}").build();

		transformer.setMakeParametersUnique(false);

		final JobLaunchRequest jobLaunchRequest = transformer.toJobLaunchRequest(message);
		final JobParameters jobParameters = jobLaunchRequest.getJobParameters();

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

		final Message<String> message = MessageBuilder.withPayload("{\"-param1\":\"Kenny\", \"-param2\":\"Cartman\"}").build();

		transformer.setMakeParametersUnique(false);
		final JobLaunchRequest jobLaunchRequest = transformer.toJobLaunchRequest(message);
		final JobParameters jobParameters = jobLaunchRequest.getJobParameters();

		assertNotNull(jobParameters);
		assertTrue(jobParameters.getParameters().size() == 2);

		assertEquals("Kenny", jobParameters.getString("param1"));
		assertEquals("Cartman", jobParameters.getString("param2"));

		for (JobParameter jobParameter : jobParameters.getParameters().values()) {
			assertFalse(jobParameter.isIdentifying());
		}
	}

	@Test
	public void createTypedJobParameters() throws Exception {

		final Message<String> message = MessageBuilder.withPayload(
				"{\"param1(long)\":\"1234\", \"mydate(date)\":\"1978-05-01\"}").build();

		transformer.setMakeParametersUnique(false);
		final JobLaunchRequest jobLaunchRequest = transformer.toJobLaunchRequest(message);
		final JobParameters jobParameters = jobLaunchRequest.getJobParameters();

		assertNotNull(jobParameters);
		assertTrue(jobParameters.getParameters().size() == 2);

		assertEquals("1234", jobParameters.getString("param1"));
		assertEquals(Long.valueOf(1234), jobParameters.getLong("param1"));

		final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		assertEquals(dateFormat.parse("1978/05/01"), jobParameters.getDate("mydate"));

	}

	@Test
	public void createTypedJobParametersWithCustomDateFormat() throws Exception {

		final Message<String> message = MessageBuilder.withPayload(
				"{\"param1\":\"We should all use ISO dates\", \"mydate(date)\":\"2013-08-15T14:50Z\"}").build();

		final DateFormat isoDateformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		transformer.setDateFormat(isoDateformat);

		final JobLaunchRequest jobLaunchRequest = transformer.toJobLaunchRequest(message);
		final JobParameters jobParameters = jobLaunchRequest.getJobParameters();

		assertNotNull(jobParameters);
		assertTrue(jobParameters.getParameters().size() == 3);
		assertEquals(isoDateformat.parse("2013-08-15T14:50Z"), jobParameters.getDate("mydate"));
	}

	@Test
	public void createTypedJobParametersWithNullDateFormat() throws Exception {

		try {
			transformer.setDateFormat(null);
		}
		catch (IllegalArgumentException e) {
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void createTypedJobParametersWithNullNumberFormat() throws Exception {

		try {
			transformer.setNumberFormat(null);
		}
		catch (IllegalArgumentException e) {
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void createTypedJobParametersWithCustomNumberFormat() throws Exception {

		final Message<String> message = MessageBuilder.withPayload("{\"param1(double)\":\"123,55\"}").build();

		final NumberFormat germanNumberFormat = NumberFormat.getInstance(Locale.GERMANY);
		transformer.setNumberFormat(germanNumberFormat);

		final JobLaunchRequest jobLaunchRequest = transformer.toJobLaunchRequest(message);
		final JobParameters jobParameters = jobLaunchRequest.getJobParameters();

		assertNotNull(jobParameters);

		assertTrue(jobParameters.getParameters().size() == 2);

		assertEquals(Double.valueOf(123.55), jobParameters.getDouble("param1"));
	}

	@Test
	public void createEmptyJobParametersWithUniqueParameter() throws Exception {

		final Message<String> message = MessageBuilder.withPayload("").build();

		final JobLaunchRequest jobLaunchRequest = transformer.toJobLaunchRequest(message);
		final JobParameters jobParameters = jobLaunchRequest.getJobParameters();

		assertNotNull(jobParameters);
		assertTrue(jobParameters.getParameters().size() == 1);
	}

	@Test
	public void createEmptyJobParametersWithOutUniqueParameter() throws Exception {

		final Message<String> message = MessageBuilder.withPayload("").build();

		transformer.setMakeParametersUnique(false);
		final JobLaunchRequest jobLaunchRequest = transformer.toJobLaunchRequest(message);
		final JobParameters jobParameters = jobLaunchRequest.getJobParameters();

		assertNotNull(jobParameters);
		assertTrue(jobParameters.getParameters().size() == 0);
	}

	@Test
	public void createJobParametersWithParamThatMatchesRandom() throws Exception {

		final Message<String> message = MessageBuilder.withPayload("{\"random\":\"Will throw Exception\"}").build();

		try {
			transformer.toJobLaunchRequest(message);
		}
		catch (IllegalStateException e) {
			return;
		}

		fail("Expected an IllegalStateException to be thrown.");
	}

	@Test
	public void createJobParametersWithInvalidJSON() throws Exception {

		final Message<String> message = MessageBuilder.withPayload("this should fail").build();

		try {
			transformer.toJobLaunchRequest(message);
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

		final Message<String> message = MessageBuilder.withPayload("{\"param1\":\"" + stringToPostInJapanese
				+ "\"}").build();

		final JobLaunchRequest jobLaunchRequest = transformer.toJobLaunchRequest(message);
		final JobParameters jobParameters = jobLaunchRequest.getJobParameters();

		assertNotNull(jobParameters);
		assertTrue(jobParameters.getParameters().size() == 2);
		assertEquals(stringToPostInJapanese, jobParameters.getString("param1"));
	}

	@Test
	public void createJobParametersWithInvalidPayloadType() throws Exception {
		final Message<Date> message = MessageBuilder.withPayload(new Date()).build();

		try {
			transformer.toJobLaunchRequest(message);
		}
		catch (IllegalArgumentException e) {
			assertEquals("This transformer does not support payloads of type "
					+ Date.class.getSimpleName(), e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testSetDateFormat() throws Exception {

		final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyy.MMMMM.dd");

		transformer.setDateFormat(simpleDateFormat);

		final SimpleDateFormat retrievedDateFormat = TestUtils.getPropertyValue(transformer,
				"jobParametersConverter.dateFormat", SimpleDateFormat.class);

		assertEquals(simpleDateFormat, retrievedDateFormat);

		transformer.setDateFormatAsString(null);

		final SimpleDateFormat retrievedDateFormat2 = TestUtils.getPropertyValue(transformer,
				"jobParametersConverter.dateFormat", SimpleDateFormat.class);

		assertEquals(simpleDateFormat, retrievedDateFormat2);

		transformer.setDateFormatAsString("yyMMdd");

		final SimpleDateFormat retrievedDateFormat3 = TestUtils.getPropertyValue(transformer,
				"jobParametersConverter.dateFormat", SimpleDateFormat.class);

		assertEquals("yyMMdd", retrievedDateFormat3.toPattern());

	}

	@Test
	public void testSetNumberFormat() throws Exception {

		final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.GERMAN);
		transformer.setNumberFormat(numberFormat);

		final NumberFormat retrievedNumberFormat = TestUtils.getPropertyValue(transformer,
				"jobParametersConverter.numberFormat", NumberFormat.class);

		assertEquals(numberFormat, retrievedNumberFormat);

		transformer.setNumberFormatAsString(null);

		final NumberFormat retrievedNumberFormat2 = TestUtils.getPropertyValue(transformer,
				"jobParametersConverter.numberFormat", NumberFormat.class);

		assertEquals(numberFormat, retrievedNumberFormat2);

		transformer.setNumberFormatAsString("#000000.000");

		final DecimalFormat retrievedDecimalFormat = TestUtils.getPropertyValue(transformer,
				"jobParametersConverter.numberFormat", DecimalFormat.class);

		assertEquals("#000000.000", retrievedDecimalFormat.toPattern());

	}

	@Test
	public void testJobParameterIncrementer() {
		jobParameterIncrementer.setKey("test-param-incrementer");
		when(mockedJob.getJobParametersIncrementer()).thenReturn(jobParameterIncrementer);
		final Message<String> message = MessageBuilder.withPayload("{\"test-param-incrementer(long)\":\"1234\"}").build();

		final JobLaunchRequest jobLaunchRequest = transformer.toJobLaunchRequest(message);

		assertNotNull(jobLaunchRequest.getJobParameters());
		assertTrue(jobLaunchRequest.getJobParameters().getParameters().size() == 2);
		// Now check if the job parameter is incremented
		assertEquals(Long.valueOf(1235), jobLaunchRequest.getJobParameters().getLong("test-param-incrementer"));
	}
}
