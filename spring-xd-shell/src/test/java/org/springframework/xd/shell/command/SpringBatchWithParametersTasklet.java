
package org.springframework.xd.shell.command;

import java.util.Map.Entry;
import java.util.Set;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.xd.shell.command.JobCommandTests.JobParametersHolder;

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
