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

import java.text.DateFormat;
import java.text.NumberFormat;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.beans.factory.InitializingBean;

/**
 * Prepares the {@link JobParameters} for the Spring Bach Jobs. As input you pass in a JSON-based Map representation of
 * the parameters. Will use the {@link DefaultJobParametersConverter} class underneath, allowing you to provide type
 * information.
 * 
 * @author Gunnar Hillert
 * @since 1.0
 * 
 */
public class JobParametersBean implements InitializingBean {

	private volatile JobParameters jobParameters;

	private volatile ExpandedJobParametersConverter jobParametersConverter = new ExpandedJobParametersConverter();

	private final String jobParametersAsJsonMap;

	/**
	 * Initializes the {@link JobParametersBean}.
	 * 
	 * @param jobParametersAsJsonMap Can be null or empty. In that case the resulting {@link JobParameters} will be
	 *        empty.
	 */
	public JobParametersBean(String jobParametersAsJsonMap) {
		this.jobParametersAsJsonMap = jobParametersAsJsonMap;
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

	/**
	 * Please ensure that {@link #afterPropertiesSet()} is called before obtaining the {@link JobParameters}.
	 * 
	 * @return The parsed Job Parameters.
	 */
	public JobParameters getJobParameters() {
		return jobParameters;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.jobParameters = this.jobParametersConverter.getJobParametersForJsonString(jobParametersAsJsonMap);
	}
}
