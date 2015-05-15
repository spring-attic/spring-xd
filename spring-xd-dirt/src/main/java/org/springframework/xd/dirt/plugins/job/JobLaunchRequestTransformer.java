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
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.xd.tuple.Tuple;

/**
 * Executes all jobs defined within a given stream once the context has been started. This really should be replaced
 * once we have the concept of triggers built in.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @since 1.0
 *
 */
public class JobLaunchRequestTransformer {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private final JobRegistry jobRegistry;

	private final String jobName;

	private volatile ExpandedJobParametersConverter jobParametersConverter = new ExpandedJobParametersConverter();

	/**
	 * @param jobName Must not be null
	 */
	public JobLaunchRequestTransformer(JobRegistry jobRegistry, String jobName) {
		Assert.notNull(jobName, "Job name must not be null.");
		this.jobRegistry = jobRegistry;
		this.jobName = jobName;
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
		Job job;
		try {
			job = jobRegistry.getJob(jobName);
		}
		catch (NoSuchJobException e) {
			throw new IllegalArgumentException("The job " + jobName + " doesn't exist. Is it deployed?");
		}
		final Object payload = message.getPayload();
		JobParameters jobParameters;

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

		final boolean isRestart = Boolean.valueOf(jobParameters.getString(ExpandedJobParametersConverter.IS_RESTART_JOB_PARAMETER_KEY));

		if (job.getJobParametersIncrementer() != null && !isRestart) {
			jobParameters = job.getJobParametersIncrementer().getNext(jobParameters);
		}

		jobParameters = jobParametersConverter.removeRestartParameterIfExists(jobParameters);

		return new JobLaunchRequest(job, jobParameters);
	}

}
