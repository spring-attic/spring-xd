/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.batch.step.tasklet.x;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.SimpleSystemProcessExitCodeMapper;
import org.springframework.batch.core.step.tasklet.SystemProcessExitCodeMapper;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Abstract tasklet for running code in a separate process and capturing the log output. The step execution
 * context will be updated with some runtime information as well as with the log output.
 *
 * Note: This this class is not thread-safe.
 *
 * @since 1.1
 * @author Thomas Rrisberg
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractProcessBuilderTasklet implements Tasklet, EnvironmentAware, StepExecutionListener {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	protected ConfigurableEnvironment environment;

	/**
	 * Exit code of job
	 */
	protected int exitCode = -1;

	private String exitMessage;

	private boolean complete = false;

	private boolean stopped = false;

	private boolean stoppable = false;

	private long checkInterval = 1000;

	private JobExplorer jobExplorer;

	private SystemProcessExitCodeMapper systemProcessExitCodeMapper = new SimpleSystemProcessExitCodeMapper();

	private List<EnvironmentProvider> environmentProviders = new ArrayList<EnvironmentProvider>();

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = (ConfigurableEnvironment) environment;
	}

	public void addEnvironmentProvider(EnvironmentProvider environmentProvider) {
		this.environmentProviders.add(environmentProvider);
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

		StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();

		String commandDescription = getCommandDescription();

		String commandName = getCommandName();

		String commandDisplayString = getCommandDisplayString();

		List<String> command = createCommand();

		File out = File.createTempFile(commandName + "-", ".out");
		stepExecution.getExecutionContext().putString(commandName.toLowerCase() + ".system.out", out.getAbsolutePath());
		logger.info(commandName + " system.out: " + out.getAbsolutePath());
		File err = File.createTempFile(commandName + "-", ".err");
		stepExecution.getExecutionContext().putString(commandName.toLowerCase() + ".system.err", err.getAbsolutePath());
		ProcessBuilder pb = new ProcessBuilder(command).redirectOutput(out).redirectError(err);
		Map<String, String> env = pb.environment();
		for (EnvironmentProvider envProvider : environmentProviders) {
			envProvider.setEnvironment(env);
		}
		String msg = commandDescription + " is being launched";
		stepExecution.getExecutionContext().putString(commandName.toLowerCase() + ".command", commandDisplayString.trim());
		List<String> commandOut = new ArrayList<String>();
		List<String> commandErr = new ArrayList<String>();
		Process p = null;
		try {
			p = pb.start();

			while (!complete) {

				try {
					exitCode = p.exitValue();
					complete = true;
				}
				catch (IllegalThreadStateException e) {
					if (stopped) {
						p.destroy();
						p = null;
						break;
					}
				}

				if (!complete) {

					if (stoppable) {
						JobExecution jobExecution =
								jobExplorer.getJobExecution(stepExecution.getJobExecutionId());

						if (jobExecution.isStopping()) {
							stopped = true;
						}
						else if (chunkContext.getStepContext().getStepExecution().isTerminateOnly()) {
							stopped = true;
						}
					}

					Thread.sleep(checkInterval);

				}

			}

			if (complete) {
				msg = commandDescription + " finished with exit code: " + exitCode;
			}
			else {
				msg = commandDescription + " was aborted due to a stop request";
			}
			if (complete && exitCode == 0) {
				logger.info(msg);
			}
			else {
				if (stopped) {
					logger.warn(msg);
				}
				else {
					logger.error(msg);
				}
			}
		}
		catch (IOException e) {
			msg = commandDescription + " job failed with: " + e;
			logger.error(msg);
		}
		catch (InterruptedException e) {
			msg = commandDescription + " job failed with: " + e;
			logger.error(msg);
		}
		finally {
			if (p != null) {
				p.destroy();
			}
			commandOut = getProcessOutput(out);
			commandErr = getProcessOutput(err);
			printLog(commandName, commandOut, commandErr);
			String firstException = getFirstExceptionMessage(commandOut, commandErr);
			if (firstException.length() > 0) {
				msg = msg + " - " + firstException;
			}
			if (commandErr.size() > 0) {
				StringBuilder commandLogErr = new StringBuilder();
				for (String line : commandErr) {
					commandLogErr.append(line).append("</br>");
				}
				stepExecution.getExecutionContext().putString(commandName.toLowerCase() + ".errors", commandLogErr.toString());
			}
			StringBuilder commandLogOut = new StringBuilder();
			for (String line : commandOut) {
				commandLogOut.append(line).append("</br>");
			}
			stepExecution.getExecutionContext().putString(commandName.toLowerCase() + ".log", commandLogOut.toString());
			exitMessage = msg;
			if (complete && exitCode != 0 || (!complete && !stopped)) {
				if (firstException.length() > 0) {
					throw new IllegalStateException("Step execution failed - " + firstException);
				}
				else {
					throw new IllegalStateException("Step execution failed - " + msg);
				}
			}
		}

		return RepeatStatus.FINISHED;

	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		exitCode = -1;
		complete = false;
		stopped = false;
		if (jobExplorer == null) {
			stoppable = false;
		}
		else {
			stoppable = isStoppable();
		}
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		if (complete) {
			return systemProcessExitCodeMapper.getExitStatus(exitCode).addExitDescription(exitMessage);
		}
		else {
			return ExitStatus.STOPPED.addExitDescription(exitMessage);
		}
	}

	protected abstract boolean isStoppable();

	protected abstract List<String> createCommand() throws Exception;

	protected abstract String getCommandDisplayString();

	protected abstract String getCommandName();

	protected abstract String getCommandDescription();

	protected List<String> getProcessOutput(File f) {
		List<String> lines = new ArrayList<String>();
		if (f == null) {
			return lines;
		}
		FileInputStream in;
		try {
			in = new FileInputStream(f);
		}
		catch (FileNotFoundException e) {
			lines.add("Failed to read log output due to " + e.getClass().getName());
			lines.add(e.getMessage());
			return lines;
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				if (lines.size() < 10000) {
					lines.add(line);
				}
				else {
					lines.add("(log output truncated)");
					break;
				}
			}
		}
		catch (IOException e) {
			lines.add("Failed to read log output due to " + e.getClass().getName());
			lines.add(e.getMessage());
		}
		finally {
			try {
				reader.close();
			}
			catch (IOException ignore) {}
		}
		return lines;
	}

	protected void printLog(String commandName, List<String> out, List<String> err) {
		if ((complete && exitCode != 0) || (!complete && !stopped)) {
			for (String line : err) {
				logger.error(commandName + " err: " + line);
			}
		}
		for (String line : out) {
			if (!complete && !stopped) {
				logger.warn(commandName + " log: " + line);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug(commandName + " log: " + line);
				}
			}
		}
	}

	protected String getFirstExceptionMessage(List<String> out, List<String> err) {
		String firstException = "";
		List<String> log = new ArrayList<String>(out);
		log.addAll(err);
		for (String line : log) {
			if (line.contains("Exception")) {
				firstException = line;
				break;
			}
		}
		return firstException;
	}

	public void setJobExplorer(JobExplorer jobExplorer) {
		this.jobExplorer = jobExplorer;
	}

	/**
	 * @param systemProcessExitCodeMapper maps system process return value to
	 * <code>ExitStatus</code> returned by Tasklet.
	 * {@link org.springframework.batch.core.step.tasklet.SimpleSystemProcessExitCodeMapper} is used by default.
	 */
	public void setSystemProcessExitCodeMapper(SystemProcessExitCodeMapper systemProcessExitCodeMapper) {
		this.systemProcessExitCodeMapper = systemProcessExitCodeMapper;
	}

	/**
	 * The time interval how often the tasklet will check for termination
	 * status.
	 *
	 * @param checkInterval time interval in milliseconds (1 second by default).
	 */
	public void setTerminationCheckInterval(long checkInterval) {
		this.checkInterval = checkInterval;
	}

}
