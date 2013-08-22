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

import java.io.File;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.Transformer;
import org.springframework.util.Assert;
import org.springframework.xd.tuple.Tuple;

/**
 * Executes all jobs defined within a given stream once the context has been started. This really should be replaced
 * once we have the concept of triggers built in.
 * 
 * @author Gunnar Hillert
 * @since 1.0
 * 
 */
public class JobLaunchRequestTransformer {

	protected final Log logger = LogFactory.getLog(getClass());

	private final Job job;

	private volatile ExpandedJobParametersConverter jobParametersConverter = new ExpandedJobParametersConverter();

	/**
	 * @param job Must not be null
	 */
	public JobLaunchRequestTransformer(Job job) {
		Assert.notNull(job, "The provided job must not be null.");
		this.job = job;
	}

	/**
	 * Will set the {@link DateFormat} on the underlying {@link DefaultJobParametersConverter}. If not set explicitly
	 * the {@link DateFormat} will default to "yyyy/MM/dd".
	 * 
	 * @param dateFormat Must not be null
	 */
	public void setDateFormat(DateFormat dateFormat) {
		this.jobParametersConverter.setDateFormat(dateFormat);
	}

	public void setDateFormatAsString(String dateFormat) {
		this.jobParametersConverter.setDateFormatAsString(dateFormat);
	}

	/**
	 * Setter for the {@link NumberFormat} which is set on the underlying {@link DefaultJobParametersConverter}. If not
	 * set explicitly, defaults to {@code NumberFormat.getInstance(Locale.US);}
	 * 
	 * @param numberFormat Must not be null.
	 */
	public void setNumberFormat(NumberFormat numberFormat) {
		this.jobParametersConverter.setNumberFormat(numberFormat);
	}

	public void setNumberFormatAsString(String numberFormat) {
		this.jobParametersConverter.setNumberFormatAsString(numberFormat);
	}

	/**
	 * 
	 * If not set, this property defaults to <code>true</code>.
	 * 
	 * @param makeParametersUnique
	 */
	public void setMakeParametersUnique(boolean makeParametersUnique) {
		this.jobParametersConverter.setMakeParametersUnique(makeParametersUnique);
	}

	@Transformer
	public JobLaunchRequest toJobLaunchRequest(Message<?> message) {

		final Object payload = message.getPayload();
		final JobParameters jobParameters;

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("JobParameters are provided as '%s'. "
					+ "Convertering to Spring Batch JobParameters...", payload.getClass().getSimpleName()));
		}

		if (payload instanceof File) {
			jobParameters = jobParametersConverter.getJobParametersForFile((File) message.getPayload());
		}
		else if (payload instanceof String) {
			jobParameters = jobParametersConverter.getJobParametersForJsonString((String) payload);
		}
		else if (payload instanceof Properties) {
			jobParameters = jobParametersConverter.getJobParameters((Properties) payload);
		}
		else if (payload instanceof Map<?, ?>) {
			jobParameters = jobParametersConverter.getJobParametersForMap((Map) payload);
		}
		else if (payload instanceof Tuple) {

			final Tuple tuple = (Tuple) payload;
			final List<Object> tupleValues = tuple.getValues();

			final Map<String, Object> map = new LinkedHashMap<String, Object>(tupleValues.size());
			for (int i = 0; i < tupleValues.size(); i++) {
				map.put(tuple.getFieldNames().get(i), tupleValues.get(i));
			}

			jobParameters = jobParametersConverter.getJobParametersForMap(map);

		}
		else {
			throw new IllegalArgumentException("This transformer does not support payloads of type "
					+ payload.getClass().getSimpleName());
		}

		return new JobLaunchRequest(job, jobParameters);
	}

}
