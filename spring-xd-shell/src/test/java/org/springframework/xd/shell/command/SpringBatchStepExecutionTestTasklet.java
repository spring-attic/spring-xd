
package org.springframework.xd.shell.command;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * Test tasklet.
 * 
 * @author Ilayaperumal Gopinathan
 */
public class SpringBatchStepExecutionTestTasklet extends StepExecutionListenerSupport implements Tasklet {

	private StepExecution stepExecution;

	@Override
	public void beforeStep(StepExecution stepExecution) {
		this.stepExecution = stepExecution;
	}

	@Override
	public RepeatStatus execute(StepContribution contribution,
			ChunkContext chunkContext) throws Exception {
		System.out.println("Running Step: " + stepExecution.getStepName());
		// The sleep allows some time for any other jobService operations happening
		// between step executions. For example: job execution stop.
		Thread.sleep(3000);
		return RepeatStatus.FINISHED;
	}
}
