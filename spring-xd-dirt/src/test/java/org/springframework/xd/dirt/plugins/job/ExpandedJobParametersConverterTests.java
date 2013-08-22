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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;

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
}
