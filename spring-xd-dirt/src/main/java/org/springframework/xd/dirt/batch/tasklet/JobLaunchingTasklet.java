/*
 * Copyright 2015 the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xd.dirt.stream.NoSuchDefinitionException;
import org.springframework.xd.dirt.stream.NotDeployedException;
import org.springframework.xd.store.DomainRepository;

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
 * @since 1.3.0
 */
public class JobLaunchingTasklet implements Tasklet, MessageHandler {

	public static final String XD_ORCHESTRATION_ID = "xd_orchestration_id";

	public static final String XD_PARENT_JOB_EXECUTION_ID = "xd_parent_execution_id";

	private long timeout;

	private long pollInterval;

	private String jobName;

	private MessageBus messageBus;

	private volatile JobExecution results = null;

	private JobDefinitionRepository definitionRepository;

	private DomainRepository<JobDefinition, String> instanceRepository;

	private String orchestrationId;

	private JobParametersExtractor extractor = new JobParametersExtractor();

	private MessageChannel launchingChannel;

	private PublishSubscribeChannel listeningChannel;

	public JobLaunchingTasklet(MessageBus messageBus,
			JobDefinitionRepository jobDefinitionRepository,
			DomainRepository instanceRepository, String jobName,
			Long timeout, Long pollInterval) {
		this(messageBus, jobDefinitionRepository, instanceRepository, jobName,
				timeout, pollInterval,
				new DirectChannel(),
				new PublishSubscribeChannel((new SimpleAsyncTaskExecutor())));
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
			DomainRepository instanceRepository, String jobName,
			Long timeout, Long pollInterval,
			MessageChannel launchingChannel, PublishSubscribeChannel listeningChannel) {
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
		this.pollInterval = pollInterval == null ? 1000 : pollInterval;
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

		configureChannels();

		validateJobDeployment();

		JobParameters originalJobParameters = chunkContext.getStepContext().getStepExecution().getJobParameters();

		String jobExecutionId = String.valueOf(chunkContext.getStepContext().getStepExecution().getJobExecution().getId());

		JobParameters jobParameters = new JobParametersBuilder(originalJobParameters)
				.addParameter(XD_ORCHESTRATION_ID, new JobParameter(this.orchestrationId))
				.addParameter(XD_PARENT_JOB_EXECUTION_ID, new JobParameter(jobExecutionId))
				.toJobParameters();

		String jobParametersString = this.extractor.extract(jobParameters);

		this.launchingChannel.send(MessageBuilder.withPayload(jobParametersString).build());

		Date startTime = new Date();

		while(results == null && !timeout(startTime)) {
			Thread.sleep(this.pollInterval);
		}

		if(results != null) {
			processResult(contribution);
		}
		else {
			throw new UnexpectedJobExecutionException("The job timed out while waiting for a result");
		}

		return RepeatStatus.FINISHED;
	}

	private void processResult(StepContribution contribution) {
		contribution.setExitStatus(results.getExitStatus());

		if(results.getStatus().isUnsuccessful()) {
			List<Throwable> allFailureExceptions = results.getAllFailureExceptions();

			Throwable failureException = null;

			if(allFailureExceptions.size() > 0) {
				failureException = allFailureExceptions.get(0);
			}

			throw new UnexpectedJobExecutionException(String.format("Step failure: %s failed.", jobName), failureException);
		}
	}

	private void configureChannels() {
		messageBus.bindPubSubConsumer(getEventListenerChannelName(jobName), this.listeningChannel, null);
		boolean subscribe = this.listeningChannel.subscribe(this);

		messageBus.bindProducer("job:" + jobName, this.launchingChannel, null);
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
		this.orchestrationId = String.valueOf(chunkContext.getStepContext().getStepExecution().getJobExecution().getJobInstance().getInstanceId());

		ExecutionContext stepExecutionContext = chunkContext.getStepContext().getStepExecution().getExecutionContext();

		if(stepExecutionContext.containsKey(XD_ORCHESTRATION_ID)) {
			this.orchestrationId = (String) stepExecutionContext.get(XD_ORCHESTRATION_ID);
		}
		else {
			stepExecutionContext.put(XD_ORCHESTRATION_ID, this.orchestrationId);
		}
	}

	private String getEventListenerChannelName(String jobName) {
		return String.format("tap:job:%s.job", jobName);
	}

	private boolean timeout(Date startTime) {
		return this.timeout >= 0 &&
				new Date().getTime() - startTime.getTime() > this.timeout;
	}

	/**
	 * Called as job events are received from the job launched in the
	 * {@link #execute(StepContribution, ChunkContext)} method.
	 *
	 * @param message The event
	 * @throws MessagingException
	 */
	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		JobExecution jobExecution = (JobExecution) message.getPayload();

		String curOrchestrationId = jobExecution.getJobParameters().getString(XD_ORCHESTRATION_ID);

		if(StringUtils.hasText(curOrchestrationId) &&
				curOrchestrationId.equalsIgnoreCase(this.orchestrationId)) {
			if(!jobExecution.isRunning()) {
				this.results = jobExecution;
			}
		}
	}

	private static class JobParametersExtractor {

		private ObjectMapper mapper = new ObjectMapper();

		public String extract(JobParameters jobParameters) throws JsonProcessingException {
			Map<String, Object> parameters = new HashMap<>(jobParameters.getParameters().size());

			for (Map.Entry<String, JobParameter> curParameterEntry : jobParameters.getParameters().entrySet()) {
				JobParameter curParameter = curParameterEntry.getValue();
				String dataTypeToUse = "(" + curParameter.getType().toString().toLowerCase() + ")";

				if(curParameter.isIdentifying()) {
					parameters.put("+" + curParameterEntry.getKey() + dataTypeToUse, curParameter.getValue());
				}
				else {
					parameters.put("-" + curParameterEntry.getKey() + dataTypeToUse, curParameter.getValue());
				}
			}

			return mapper.writeValueAsString(parameters);
		}
	}
}
