/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.xd.dirt.batch.tasklet;

import java.util.Date;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.integration.bus.BusUtils;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.integration.bus.MessageBus.Capability;
import org.springframework.xd.dirt.plugins.job.ExpandedJobParametersConverter;
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xd.dirt.stream.NoSuchDefinitionException;
import org.springframework.xd.dirt.stream.NotDeployedException;
import org.springframework.xd.store.DomainRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A {@link Tasklet} implementation that uses the Spring XD {@link MessageBus} to launch
 * jobs deployed within the same Spring XD cluster.  This tasklet also receives the
 * results of the job from the same message bus.
 *
 * The result of the step executing this tasklet should match that of the job that was
 * executed by this job.  So if the tasklet executes the job foo and foo returns an
 * {@link org.springframework.batch.core.ExitStatus} of "BAR", the ExitStatus of this step
 * will also be "BAR".
 *
 * @author Michael Minella
 * @author Gary Russell
 * @since 1.3.0
 */
public class JobLaunchingTasklet implements Tasklet {

	private final Logger logger = LoggerFactory.getLogger(JobLaunchingTasklet.class);

	public static final String XD_ORCHESTRATION_ID = "xd_orchestration_id";

	public static final String XD_PARENT_JOB_EXECUTION_ID = "xd_parent_execution_id";

	private long timeout;

	private String jobName;

	private MessageBus messageBus;

	private JobDefinitionRepository definitionRepository;

	private DomainRepository<JobDefinition, String> instanceRepository;

	private String orchestrationId;

	private MessageChannel launchingChannel;

	private PollableChannel listeningChannel;

	public JobLaunchingTasklet(MessageBus messageBus,
			JobDefinitionRepository jobDefinitionRepository,
			DomainRepository<JobDefinition, String>  instanceRepository, String jobName,
			Long timeout) {
		this(messageBus, jobDefinitionRepository, instanceRepository, jobName,
				timeout,
				createLaunchingChannel(jobName),
				createListeningChannel(jobName));
	}

	/**
	 * Provided for testing to be able to inject the channels for mocking.
	 *
	 * @param messageBus Message bus reference to launch a job and receive it's results
	 * @param jobDefinitionRepository Repository used to look up the child job definition
	 * @param instanceRepository Repository used to look up that the child job is deployed
	 * @param jobName The name of the child job definition
	 * @param launchingChannel The channel used to send the launch request
	 * @param listeningChannel The channel used to listen for the job results
	 */
	protected JobLaunchingTasklet(MessageBus messageBus,
			JobDefinitionRepository jobDefinitionRepository,
			DomainRepository<JobDefinition, String>  instanceRepository, String jobName,
			Long timeout,
			MessageChannel launchingChannel, PollableChannel listeningChannel) {
		Assert.notNull(messageBus, "A message bus is required");
		Assert.notNull(jobDefinitionRepository, "A JobDefinitionRepository is required");
		Assert.notNull(instanceRepository, "A DomainRepository is required");
		Assert.notNull(jobName, "A job name is required");

		this.jobName = jobName;
		this.messageBus = messageBus;
		this.definitionRepository = jobDefinitionRepository;
		this.instanceRepository = instanceRepository;
		this.launchingChannel = launchingChannel;
		this.listeningChannel = listeningChannel;

		this.timeout = timeout == null ? -1 : timeout;
	}

	private static DirectChannel createLaunchingChannel(String jobName) {
		DirectChannel launchingChannel = new DirectChannel();
		launchingChannel.setBeanName(jobName + ":launcher");
		return launchingChannel;
	}

	private static QueueChannel createListeningChannel(String jobName) {
		QueueChannel listeningChannel = new QueueChannel();
		listeningChannel.setBeanName(jobName + ":resultListener");
		return listeningChannel;
	}

	/**
	 * Uses the {@link MessageBus} to execute a job deployed in a Spring XD cluster.
	 *
	 * @param contribution The contribution to the step's metrics.  Not used in this case
	 * @param chunkContext Used to get a handle on the JobExecution and JobInstance
	 * @return RepeatStatus.FINISHED
	 * @throws Exception
	 */
	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

		setOrchestrationId(chunkContext);

		String driverJobName = chunkContext.getStepContext().getStepExecution().getJobExecution().getJobInstance()
				.getJobName();

		bindChannels(driverJobName);

		try {
			validateJobDeployment();

			String jobParametersString = getJobParameters(chunkContext);

			logger.debug("Launching request for {} orchestration {}", this.jobName, this.orchestrationId);

			this.launchingChannel.send(MessageBuilder.withPayload(jobParametersString).build());

			Date startTime = new Date();

			long remaining = this.timeout;
			JobExecution results = null;
			while (results == null && (this.timeout > 0 ? remaining > 0 : true)) {
				Message<?> resultMessage = this.timeout > 0 ? this.listeningChannel.receive(remaining)
						: this.listeningChannel.receive();
				if (resultMessage != null) {
					results = getResult(resultMessage);
				}
				remaining = startTime.getTime() - System.currentTimeMillis() + this.timeout;
			}

			if (results != null) {
				processResult(contribution, chunkContext, results);
			}
			else {
				throw new UnexpectedJobExecutionException("The job timed out while waiting for a result");
			}

			logger.debug("Completed processing for {} orchestration {}", this.jobName, this.orchestrationId);

			return RepeatStatus.FINISHED;
		}
		finally {
			unbindChannels(driverJobName);
		}
	}

	private String getJobParameters(ChunkContext chunkContext) throws JsonProcessingException {

		StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();
		JobParameters originalJobParameters = stepExecution.getJobParameters();

		Properties currentJobParameters = originalJobParameters.toProperties();

		currentJobParameters.remove(ExpandedJobParametersConverter.UNIQUE_JOB_PARAMETER_KEY);

		String random = null;

		//This is a restart
		Map<String, Object> stepExecutionContext = chunkContext.getStepContext().getStepExecutionContext();
		if(stepExecutionContext.containsKey(ExpandedJobParametersConverter.UNIQUE_JOB_PARAMETER_KEY)) {
			random = (String) stepExecutionContext.get(ExpandedJobParametersConverter.UNIQUE_JOB_PARAMETER_KEY);
		}

		currentJobParameters.put(XD_ORCHESTRATION_ID, this.orchestrationId);
		currentJobParameters.put("-" + XD_PARENT_JOB_EXECUTION_ID, stepExecution.getJobExecutionId());

		if(random != null) {
			currentJobParameters.put(ExpandedJobParametersConverter.IS_RESTART_JOB_PARAMETER_KEY, true);
			currentJobParameters.put(ExpandedJobParametersConverter.UNIQUE_JOB_PARAMETER_KEY, random);
		}

		return new ObjectMapper().writeValueAsString(currentJobParameters);
	}

	private void processResult(StepContribution contribution, ChunkContext chunkContext, JobExecution results) {
		contribution.setExitStatus(results.getExitStatus());
		if (results.getStatus().isUnsuccessful()) {
			chunkContext.getStepContext()
					.getStepExecution()
					.getExecutionContext()
					.put(ExpandedJobParametersConverter.UNIQUE_JOB_PARAMETER_KEY,
							results.getJobParameters().getString(ExpandedJobParametersConverter.UNIQUE_JOB_PARAMETER_KEY));
			throw new UnexpectedJobExecutionException(String.format("Step failure: %s failed.", jobName));
		}
	}

	private void bindChannels(String driverJobName) {
		messageBus.bindPubSubConsumer(getEventListenerChannelName(this.jobName, driverJobName), this.listeningChannel, null);
		messageBus.bindProducer("job:" + jobName, this.launchingChannel, null);
	}

	private void unbindChannels(String driverJobName) {
		messageBus.unbindConsumer(getEventListenerChannelName(jobName, driverJobName), this.listeningChannel);
		messageBus.unbindProducer("job:" + jobName, this.launchingChannel);
	}

	private void validateJobDeployment() {
		// Double check so that user gets an informative error message
		JobDefinition job = definitionRepository.findOne(jobName);
		if (job == null) {
			throw new NoSuchDefinitionException(jobName,
					String.format("There is no %s definition named '%%s'", "job"));
		}
		if (instanceRepository.findOne(jobName) == null) {
			throw new NotDeployedException(jobName, String.format("The %s named '%%s' is not currently deployed",
					"job"));
		}
	}

	private void setOrchestrationId(ChunkContext chunkContext) {
		this.orchestrationId = String.valueOf(
				chunkContext.getStepContext().getStepExecution().getJobExecution().getJobInstance().getInstanceId());

		ExecutionContext stepExecutionContext = chunkContext.getStepContext().getStepExecution().getExecutionContext();

		if (stepExecutionContext.containsKey(XD_ORCHESTRATION_ID)) {
			this.orchestrationId = (String) stepExecutionContext.get(XD_ORCHESTRATION_ID);
		}
		else {
			stepExecutionContext.put(XD_ORCHESTRATION_ID, this.orchestrationId);
		}
	}

	private String getEventListenerChannelName(String jobName, String driverJobName) {
		String tapName = String.format("tap:job:%s.job", jobName);
		if (this.messageBus.isCapable(Capability.DURABLE_PUBSUB)) {
			tapName = BusUtils.addGroupToPubSub(driverJobName, tapName);
		}
		return tapName;
	}

	public JobExecution getResult(Message<?> message) throws MessagingException {
		JobExecution jobExecution = (JobExecution) message.getPayload();

		String curOrchestrationId = jobExecution.getJobParameters().getString(XD_ORCHESTRATION_ID);

		logger.debug("Received result for {} orchestration {}", this.jobName, curOrchestrationId);

		if (StringUtils.hasText(curOrchestrationId) &&
				curOrchestrationId.equalsIgnoreCase(this.orchestrationId)) {
			if (!jobExecution.isRunning()) {
				return jobExecution;
			}
		}
		return null;
	}
}
