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

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;

/**
 * Prepares the {@link JobParameters} for the Spring Bach Jobs. As input you pass
 * in a JSON-based Map representation of the parameters. Will use the
 * {@link DefaultJobParametersConverter} class underneath, allowing you to provide
 * type information.
 *
 * @author Gunnar Hillert
 * @since 1.0
 *
 */
public class JobParametersBean implements InitializingBean {

	private final Log logger = LogFactory.getLog(getClass());

	private static final String UNIQUE_JOB_PARAMETER_KEY = "random";

	private final ObjectMapper objectMapper = new ObjectMapper();

	private volatile JobParameters jobParameters;

	private volatile DateFormat dateFormat = null;

	private volatile NumberFormat numberFormat = null;

	private volatile boolean makeParametersUnique = true;

	private volatile DefaultJobParametersConverter jobParametersConverter = new DefaultJobParametersConverter();

	private final String jobParametersAsJsonMap;

	/**
	 * Initializes the {@link JobParametersBean}.
	 *
	 * @param jobParametersAsJsonMap Can be null or empty. In that case the resulting
	 * {@link JobParameters} will be empty.
	 */
	public JobParametersBean(String jobParametersAsJsonMap) {
		this.jobParametersAsJsonMap = jobParametersAsJsonMap;
	}

	/**
	 * Will set the {@link DateFormat} on the underlying {@link DefaultJobParametersConverter}.
	 * If not set explicitly the {@link DateFormat} will default to "yyyy/MM/dd".
	 *
	 * @param dateFormat Must not be null
	 */
	public void setDateFormat(DateFormat dateFormat) {
		Assert.notNull(dateFormat, "The provided dateFormat must not be null.");
		this.dateFormat = dateFormat;
	}

	public void setDateFormatAsString(String dateFormat) {
		if (StringUtils.hasText(dateFormat)) {
			this.dateFormat = new SimpleDateFormat(dateFormat);
		}
	}

	/**
	 * Setter for the {@link NumberFormat} which is set on the underlying {@link DefaultJobParametersConverter}.
	 * If not set explicitly, defaults to {@code NumberFormat.getInstance(Locale.US);}
	 *
	 * @param numberFormat Must not be null.
	 */
	public void setNumberFormat(NumberFormat numberFormat) {
		Assert.notNull(numberFormat, "The provided numberFormat must not be null.");
		this.numberFormat = numberFormat;
	}

	public void setNumberFormatAsString(String numberFormat) {
		if (StringUtils.hasText(numberFormat)) {
			this.numberFormat = new DecimalFormat(numberFormat);
		}
	}

	/**
	 *
	 * If not set, this property defaults to <code>true</code>.
	 * @param makeParametersUnique
	 */
	public void setMakeParametersUnique(boolean makeParametersUnique) {
		this.makeParametersUnique = makeParametersUnique;
	}

	/**
	 * Please ensure that {@link #afterPropertiesSet()} is called before obtaining
	 * the {@link JobParameters}.
	 *
	 * @return The parsed Job Parameters.
	 */
	public JobParameters getJobParameters() {
		return jobParameters;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

 		if (this.dateFormat != null) {
			jobParametersConverter.setDateFormat(dateFormat);
		}

		if (this.numberFormat != null) {
			jobParametersConverter.setNumberFormat(numberFormat);
		}

		final Properties parametersAsProperties = new Properties();

		if (this.makeParametersUnique) {
			parametersAsProperties.put("random", String.valueOf(Math.random()));
		}

		if (jobParametersAsJsonMap != null && !jobParametersAsJsonMap.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("JobParameters in JSON format are being passed in. Convertering to Spring Batch JobParameters...");
			}

			final Map<String, Object> parameters;
			final MapType mapType = objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class,
					String.class);

			try {
				parameters = new ObjectMapper().readValue(jobParametersAsJsonMap, mapType);
			}
			catch (IOException e) {
				throw new IllegalArgumentException("Unable to convert provided JSON to Map<String, Object>", e);
			}

			if (parameters.containsKey(UNIQUE_JOB_PARAMETER_KEY)) {
				throw new IllegalStateException(String.format(
						"Parameter '%s' is already used to identify uniqueness for the executing Batch job.",
						UNIQUE_JOB_PARAMETER_KEY));
			}

			parametersAsProperties.putAll(parameters);
		}

		this.jobParameters = jobParametersConverter.getJobParameters(parametersAsProperties);

	}
}
