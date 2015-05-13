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

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.rest.domain.util.TimeUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;

/**
 * More flexible implementation of the {@link JobParametersConverter}. Allows to convert a wide variety of object types
 * to {@link JobParameters}.
 *
 * @author Gunnar Hillert
 * @since 1.0
 *
 */
public class ExpandedJobParametersConverter extends DefaultJobParametersConverter {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String ABSOLUTE_FILE_PATH = "absoluteFilePath";

	public static final String UNIQUE_JOB_PARAMETER_KEY = "random";

	public static final String IS_RESTART_JOB_PARAMETER_KEY = "XD_isRestart";

	private volatile boolean makeParametersUnique = true;

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Default Constructor, initializing {@link DefaultJobParametersConverter#setDateFormat(DateFormat)}
	 * with {@link TimeUtils#getDefaultDateFormat()}.
	 */
	public ExpandedJobParametersConverter() {
		this.setDateFormat(TimeUtils.getDefaultDateFormat());
	}

	/**
	 * Will set the {@link DateFormat} on the underlying {@link DefaultJobParametersConverter}. If not set explicitly,
	 * the {@link DateFormat} will default to {@link TimeUtils#getDefaultDateFormat()}.
	 *
	 * @param dateFormat Must not be null
	 */
	@Override
	public void setDateFormat(DateFormat dateFormat) {
		Assert.notNull(dateFormat, "The provided dateFormat must not be null.");
		super.setDateFormat(dateFormat);
	}

	/**
	 * Allows for setting the {@link DateFormat} using a {@link String}. If not
	 * set, the default {@link DateFormat} used will be {@link TimeUtils#getDefaultDateFormat()}.
	 *
	 * @param dateFormatAsString Will be ignored if null or empty.
	 */
	public void setDateFormatAsString(String dateFormatAsString) {
		if (StringUtils.hasText(dateFormatAsString)) {
			super.setDateFormat(new SimpleDateFormat(dateFormatAsString));
		}
	}

	/**
	 * If not set, this property defaults to <code>true</code>.
	 *
	 * @param makeParametersUnique If not set defaults to {@code true}
	 */
	public void setMakeParametersUnique(boolean makeParametersUnique) {
		this.makeParametersUnique = makeParametersUnique;
	}

	/**
	 * Setter for the {@link NumberFormat} which is set on the underlying {@link DefaultJobParametersConverter}. If not
	 * set explicitly, defaults to {@code NumberFormat.getInstance(Locale.US);}
	 *
	 * @param numberFormat Must not be null.
	 */
	@Override
	public void setNumberFormat(NumberFormat numberFormat) {
		Assert.notNull(numberFormat, "The provided numberFormat must not be null.");
		super.setNumberFormat(numberFormat);
	}

	/**
	 * Allows for setting the {@link NumberFormat} using a {@link String}. The passed-in String will be converted to a
	 * {@link DecimalFormat}.
	 *
	 * @param numberFormatAsString Will be ignored if null or empty.
	 */
	public void setNumberFormatAsString(String numberFormatAsString) {
		if (StringUtils.hasText(numberFormatAsString)) {
			super.setNumberFormat(new DecimalFormat(numberFormatAsString));
		}
	}

	/**
	 * Return {@link JobParameters} for the passed-in {@link File}. Will set the {@link JobParameter} with key
	 * {@link #ABSOLUTE_FILE_PATH} to the {@link File}'s absolutePath. Method will ultimately call
	 * {@link #getJobParameters(Properties)}.
	 *
	 * @param file Must not be null.
	 */
	public JobParameters getJobParametersForFile(File file) {
		Assert.notNull(file, "The provided file must not be null.");
		final Properties parametersAsProperties = new Properties();
		parametersAsProperties.put(ABSOLUTE_FILE_PATH, file.getAbsolutePath());
		return this.getJobParameters(parametersAsProperties);
	}

	/**
	 * Converts a {@link String}-based JSON map to {@link JobParameters}. The String is converted using Jackson's
	 * {@link ObjectMapper}.
	 *
	 * The method will ultimately call {@link #getJobParametersForMap(Map)}.
	 *
	 * @param jobParametersAsJsonMap Can be null or empty.
	 */
	public JobParameters getJobParametersForJsonString(String jobParametersAsJsonMap) {

		final Map<String, Object> parameters;

		if (jobParametersAsJsonMap != null && !jobParametersAsJsonMap.isEmpty()) {

			final MapType mapType = objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class,
					String.class);

			try {
				parameters = new ObjectMapper().readValue(jobParametersAsJsonMap, mapType);
			}
			catch (IOException e) {
				throw new IllegalArgumentException("Unable to convert provided JSON to Map<String, Object>", e);
			}

		}
		else {
			parameters = null;
		}

		return getJobParametersForMap(parameters);
	}

	/**
	 * Will convert the provided {@link Map} into {@link JobParameters}. The method will ultimately call
	 * {@link #getJobParameters(Properties)}.
	 *
	 * @param map Can be null or an empty {@link Map}.
	 */
	public JobParameters getJobParametersForMap(Map<?, ?> map) {

		final Properties parametersAsProperties = new Properties();

		if (map != null) {
			parametersAsProperties.putAll(map);
		}

		return this.getJobParameters(parametersAsProperties);
	}

	/**
	 * If {@link #makeParametersUnique} is {@code true} the {@link JobParameter} with key
	 * {@link #UNIQUE_JOB_PARAMETER_KEY} will be added with a random number value.
	 *
	 * The method will ultimately call {@link DefaultJobParametersConverter#getJobParameters(Properties)}.
	 *
	 * @param properties Can be null.
	 */
	@Override
	public JobParameters getJobParameters(Properties properties) {

		final Properties localProperties;

		if (properties != null) {
			localProperties = properties;
		}
		else {
			localProperties = new Properties();
		}

		final boolean isRestart;

		if (localProperties.containsKey(IS_RESTART_JOB_PARAMETER_KEY)) {
			isRestart = Boolean.valueOf(localProperties.getProperty(IS_RESTART_JOB_PARAMETER_KEY));
		}
		else {
			isRestart = false;
		}

		if (this.makeParametersUnique && !isRestart) {

			if (localProperties.containsKey(UNIQUE_JOB_PARAMETER_KEY)) {
				throw new IllegalStateException(String.format(
						"Parameter '%s' is already used to identify uniqueness for the executing Batch job.",
						UNIQUE_JOB_PARAMETER_KEY));
			}

			localProperties.put(UNIQUE_JOB_PARAMETER_KEY, String.valueOf(Math.random()));
		}
		return super.getJobParameters(localProperties);
	}

	/**
	 * This method will convert {@link JobParameters} to a JSON String. The parameters in the resulting JSON String are
	 * sorted by the name of the parameters.
	 *
	 * This method will delegate to {@link #getJobParametersAsString(JobParameters, boolean)}
	 *
	 * @param jobParameters Must not be null
	 * @return A JSON String representation of the {@link JobParameters}
	 */
	public String getJobParametersAsString(JobParameters jobParameters) {
		return this.getJobParametersAsString(jobParameters, false);
	}

	/**
	 * This method will convert {@link JobParameters} to a JSON String. The parameters in the resulting JSON String are
	 * sorted by the name of the parameters.
	 *
	 * @param jobParameters Must not be null
	 * @param isRestart When {@code true}, add a restart flag
	 * @return A JSON String representation of the {@link JobParameters}
	 */
	public String getJobParametersAsString(JobParameters jobParameters, boolean isRestart) {

		Assert.notNull(jobParameters, "jobParameters must not be null.");

		final Properties properties = this.getProperties(jobParameters);

		if (isRestart) {
			properties.put(IS_RESTART_JOB_PARAMETER_KEY, Boolean.TRUE.toString());
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		final SortedMap<String, String> sortedJobParameters = new TreeMap(properties);

		final String jobParametersAsString;

		try {
			jobParametersAsString = new ObjectMapper().writeValueAsString(sortedJobParameters);
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Unable to convert provided job parameters to JSON String.", e);
		}
		return jobParametersAsString;
	}

	/**
	 * If {@link JobParameters} contains a parameters named {@value #IS_RESTART_JOB_PARAMETER_KEY} removed it.
	 *
	 * @param jobParameters Must not be null
	 * @return A new instance of {@link JobParameters}
	 */
	public JobParameters removeRestartParameterIfExists(JobParameters jobParameters) {

		Assert.notNull(jobParameters, "'jobParameters' must not be null.");

		final JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();

		for (Map.Entry<String, JobParameter> entry : jobParameters.getParameters().entrySet()) {
			if (!IS_RESTART_JOB_PARAMETER_KEY.equalsIgnoreCase(entry.getKey())) {
				jobParametersBuilder.addParameter(entry.getKey(), entry.getValue());
			}
		}

		return jobParametersBuilder.toJobParameters();
	}
}
