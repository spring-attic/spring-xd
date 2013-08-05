package org.springframework.xd.shell.command;

import java.io.File;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class HelloSpringXDTasklet implements Tasklet {

	@Override
	public RepeatStatus execute(StepContribution contribution,
			ChunkContext chunkContext) throws Exception {

		System.out.println("Hello Spring XD Part Deux!");
		File file = new File("./src/test/resources/TMPTESTFILE.txt");
		if (!file.exists()) {
			file.createNewFile();
		}
		return RepeatStatus.FINISHED;
	}
}
