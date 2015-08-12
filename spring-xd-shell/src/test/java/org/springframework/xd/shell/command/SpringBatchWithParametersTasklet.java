/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.shell.command;

import java.util.Map.Entry;
import java.util.Set;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.xd.shell.command.AbstractJobCommandsTests.JobParametersHolder;

public class SpringBatchWithParametersTasklet implements Tasklet {

	@Override
	public RepeatStatus execute(StepContribution contribution,
			ChunkContext chunkContext) throws Exception {

		System.out.println("Hello Spring XD With Job Parameters!");

		final JobParametersHolder jobParametersHolder = new JobParametersHolder();

		final JobParameters jobParameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
		final Set<Entry<String, JobParameter>> parameterEntries = jobParameters.getParameters().entrySet();

		for (Entry<String, JobParameter> jobParameterEntry : parameterEntries) {
			jobParametersHolder.addParameter(jobParameterEntry.getKey(), jobParameterEntry.getValue());
		}

		jobParametersHolder.countDown();

		return RepeatStatus.FINISHED;
	}
}
