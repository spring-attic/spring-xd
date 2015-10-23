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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.amqp.channel.PublishSubscribeAmqpChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xd.dirt.stream.NoSuchDefinitionException;
import org.springframework.xd.dirt.stream.NotDeployedException;
import org.springframework.xd.store.DomainRepository;

/**
 * @author Michael Minella
 * @author Gary Russell
 */
public class JobLaunchingTaskletTests {

	private JobLaunchingTasklet tasklet;

	@Mock
	private MessageBus bus;

	@Mock
	private JobDefinitionRepository jobDefinitionRepository;

	@Mock
	private DomainRepository<JobDefinition, String> instanceRepository;

	@Mock
	private MessageChannel launchingChannel;

	@Spy
	private PollableChannel listeningChannel = new QueueChannel();

	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		this.tasklet = new JobLaunchingTasklet(bus, jobDefinitionRepository, instanceRepository, "foo", null,
				launchingChannel, listeningChannel);
	}

	@Test
	public void testInitialLaunch() throws Exception {
		JobInstance jobInstance = new JobInstance(3l, "masterFoo");
		JobExecution jobExecution = new JobExecution(5l);
		jobExecution.setJobInstance(jobInstance);
		StepExecution stepExecution = new StepExecution("masterFoo", jobExecution, 7l);
		final StepContribution stepContribution = new StepContribution(stepExecution);
		final ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

		JobDefinition jobDefinition = new JobDefinition("foo", "foo");
		when(jobDefinitionRepository.findOne("foo")).thenReturn(jobDefinition);
		when(instanceRepository.findOne("foo")).thenReturn(jobDefinition);
		doReturn(true).when(launchingChannel).send(any(Message.class));

		FutureTask<RepeatStatus> result = new FutureTask<RepeatStatus>(new Callable<RepeatStatus>() {

			@Override
			public RepeatStatus call() throws Exception {
				return tasklet.execute(stepContribution, chunkContext);
			}
		});

		taskExecutor.execute(result);

		JobParameters jobParameters = new JobParametersBuilder().addString(JobLaunchingTasklet.XD_ORCHESTRATION_ID,
				"3").toJobParameters();
		JobExecution slaveExecution = new JobExecution(9l, jobParameters);
		slaveExecution.setStatus(BatchStatus.COMPLETED);
		slaveExecution.setEndTime(new Date());

		this.listeningChannel.send(MessageBuilder.withPayload(slaveExecution).build());

		assertEquals(result.get(5, TimeUnit.SECONDS), RepeatStatus.FINISHED);

		assertEquals(stepExecution.getExecutionContext().get(JobLaunchingTasklet.XD_ORCHESTRATION_ID), "3");

		verify(bus).bindPubSubConsumer(eq("tap:job:foo.job"), any(PublishSubscribeAmqpChannel.class),
				(Properties) isNull());
	}

	@Test
	public void testInitialLaunchWithOneJobParameter()
			throws ExecutionException, InterruptedException, TimeoutException {
		JobParameters originalJobParameters = new JobParametersBuilder().addString("bar", "baz").toJobParameters();
		JobInstance jobInstance = new JobInstance(3l, "masterFoo");
		JobExecution jobExecution = new JobExecution(5l, originalJobParameters);
		jobExecution.setJobInstance(jobInstance);
		StepExecution stepExecution = new StepExecution("masterFoo", jobExecution, 7l);
		final StepContribution stepContribution = new StepContribution(stepExecution);
		final ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

		JobDefinition jobDefinition = new JobDefinition("foo", "foo");
		when(jobDefinitionRepository.findOne("foo")).thenReturn(jobDefinition);
		when(instanceRepository.findOne("foo")).thenReturn(jobDefinition);
		doReturn(true).when(launchingChannel).send(any(Message.class));

		FutureTask<RepeatStatus> result = new FutureTask<RepeatStatus>(new Callable<RepeatStatus>() {

			@Override
			public RepeatStatus call() throws Exception {
				assertEquals(chunkContext.getStepContext().getStepExecution().getJobParameters().getString("bar"),
						"baz");
				return tasklet.execute(stepContribution, chunkContext);
			}
		});

		taskExecutor.execute(result);

		JobParameters jobParameters = new JobParametersBuilder().addString(JobLaunchingTasklet.XD_ORCHESTRATION_ID,
				"3").toJobParameters();
		JobExecution slaveExecution = new JobExecution(9l, jobParameters);
		slaveExecution.setStatus(BatchStatus.COMPLETED);
		slaveExecution.setEndTime(new Date());

		//This is to allow the job to "start" before mocking the sending of the event
		Thread.sleep(1000l);

		this.listeningChannel.send(MessageBuilder.withPayload(slaveExecution).build());

		assertEquals(result.get(5, TimeUnit.SECONDS), RepeatStatus.FINISHED);

		assertEquals(stepExecution.getExecutionContext().get(JobLaunchingTasklet.XD_ORCHESTRATION_ID), "3");

		verify(bus).bindPubSubConsumer(eq("tap:job:foo.job"), any(PublishSubscribeAmqpChannel.class),
				(Properties) isNull());
	}

	@Test
	public void testInitialLaunchWithThreeJobParameters()
			throws ExecutionException, InterruptedException, TimeoutException {
		JobParameters originalJobParameters = new JobParametersBuilder()
				.addString("string", "baz")
				.addLong("long", 5l)
				.addDate("date", new Date(1l))
				.toJobParameters();
		JobInstance jobInstance = new JobInstance(3l, "masterFoo");
		JobExecution jobExecution = new JobExecution(5l, originalJobParameters);
		jobExecution.setJobInstance(jobInstance);
		StepExecution stepExecution = new StepExecution("masterFoo", jobExecution, 7l);
		final StepContribution stepContribution = new StepContribution(stepExecution);
		final ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

		JobDefinition jobDefinition = new JobDefinition("foo", "foo");
		when(jobDefinitionRepository.findOne("foo")).thenReturn(jobDefinition);
		when(instanceRepository.findOne("foo")).thenReturn(jobDefinition);
		doReturn(true).when(launchingChannel).send(any(Message.class));

		FutureTask<RepeatStatus> result = new FutureTask<RepeatStatus>(new Callable<RepeatStatus>() {

			@Override
			public RepeatStatus call() throws Exception {
				JobParameters jobParameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
				assertEquals(jobParameters.getString("string"), "baz");
				assertEquals((long) jobParameters.getLong("long"), 5l);
				assertEquals(jobParameters.getDate("date"), new Date(1));

				return tasklet.execute(stepContribution, chunkContext);
			}
		});

		taskExecutor.execute(result);

		JobParameters jobParameters = new JobParametersBuilder().addString(JobLaunchingTasklet.XD_ORCHESTRATION_ID,
				"3").toJobParameters();
		JobExecution slaveExecution = new JobExecution(9l, jobParameters);
		slaveExecution.setStatus(BatchStatus.COMPLETED);
		slaveExecution.setEndTime(new Date());

		this.listeningChannel.send(MessageBuilder.withPayload(slaveExecution).build());

		assertEquals(result.get(5, TimeUnit.SECONDS), RepeatStatus.FINISHED);

		assertEquals(stepExecution.getExecutionContext().get(JobLaunchingTasklet.XD_ORCHESTRATION_ID), "3");

		verify(bus).bindPubSubConsumer(eq("tap:job:foo.job"), any(PublishSubscribeAmqpChannel.class),
				(Properties) isNull());
	}

	@Test
	public void testJobNotDefined() throws InterruptedException, ExecutionException, TimeoutException {
		JobInstance jobInstance = new JobInstance(3l, "masterFoo");
		JobExecution jobExecution = new JobExecution(5l);
		jobExecution.setJobInstance(jobInstance);
		StepExecution stepExecution = new StepExecution("masterFoo", jobExecution, 7l);
		final StepContribution stepContribution = new StepContribution(stepExecution);
		final ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

		FutureTask<RepeatStatus> result = new FutureTask<RepeatStatus>(new Callable<RepeatStatus>() {

			@Override
			public RepeatStatus call() throws Exception {
				return tasklet.execute(stepContribution, chunkContext);
			}
		});

		taskExecutor.execute(result);

		try {
			result.get(5, TimeUnit.SECONDS);
		}
		catch (ExecutionException e) {
			Throwable cause = e.getCause();
			assertEquals(cause.getClass(), NoSuchDefinitionException.class);
			assertEquals(cause.getMessage(), "There is no job definition named 'foo'");
		}
	}

	@Test
	public void testJobNotDeployed() throws TimeoutException, InterruptedException {
		JobInstance jobInstance = new JobInstance(3l, "masterFoo");
		JobExecution jobExecution = new JobExecution(5l);
		jobExecution.setJobInstance(jobInstance);
		StepExecution stepExecution = new StepExecution("masterFoo", jobExecution, 7l);
		final StepContribution stepContribution = new StepContribution(stepExecution);
		final ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

		JobDefinition jobDefinition = new JobDefinition("foo", "foo");
		when(jobDefinitionRepository.findOne("foo")).thenReturn(jobDefinition);

		FutureTask<RepeatStatus> result = new FutureTask<RepeatStatus>(new Callable<RepeatStatus>() {

			@Override
			public RepeatStatus call() throws Exception {
				return tasklet.execute(stepContribution, chunkContext);
			}
		});

		taskExecutor.execute(result);

		try {
			result.get(5, TimeUnit.SECONDS);
		}
		catch (ExecutionException e) {
			Throwable cause = e.getCause();
			assertEquals(cause.getClass(), NotDeployedException.class);
			assertEquals(cause.getMessage(), "The job named 'foo' is not currently deployed");
		}
	}

	@Test
	public void testJobFailed() throws InterruptedException, ExecutionException, TimeoutException {
		JobInstance jobInstance = new JobInstance(3l, "masterFoo");
		JobExecution jobExecution = new JobExecution(5l);
		jobExecution.setJobInstance(jobInstance);
		StepExecution stepExecution = new StepExecution("masterFoo", jobExecution, 7l);
		final StepContribution stepContribution = new StepContribution(stepExecution);
		final ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

		JobDefinition jobDefinition = new JobDefinition("foo", "foo");
		when(jobDefinitionRepository.findOne("foo")).thenReturn(jobDefinition);
		when(instanceRepository.findOne("foo")).thenReturn(jobDefinition);

		FutureTask<RepeatStatus> result = new FutureTask<RepeatStatus>(new Callable<RepeatStatus>() {

			@Override
			public RepeatStatus call() throws Exception {
				return tasklet.execute(stepContribution, chunkContext);
			}
		});

		taskExecutor.execute(result);

		JobParameters jobParameters = new JobParametersBuilder().addString(JobLaunchingTasklet.XD_ORCHESTRATION_ID,
				"3").toJobParameters();
		JobExecution slaveExecution = new JobExecution(9l, jobParameters);
		slaveExecution.setStatus(BatchStatus.FAILED);
		slaveExecution.setExitStatus(new ExitStatus("Job Failure"));
		slaveExecution.setEndTime(new Date());

		//This is to allow the job to "start" before mocking the sending of the event
		Thread.sleep(1000l);

		this.listeningChannel.send(MessageBuilder.withPayload(slaveExecution).build());

		try {
			assertEquals(result.get(5, TimeUnit.SECONDS), RepeatStatus.FINISHED);
		}
		catch (ExecutionException e) {
			Throwable cause = e.getCause();
			assertEquals(cause.getClass(), UnexpectedJobExecutionException.class);
			assertEquals(cause.getMessage(), "Step failure: foo failed.");
		}

		assertEquals(stepExecution.getExecutionContext().get(JobLaunchingTasklet.XD_ORCHESTRATION_ID), "3");

		verify(bus).bindPubSubConsumer(eq("tap:job:foo.job"), any(PublishSubscribeAmqpChannel.class),
				(Properties) isNull());
	}

	@Test
	public void testRestart() throws InterruptedException, ExecutionException, TimeoutException {
		JobInstance jobInstance = new JobInstance(3l, "masterFoo");
		JobExecution jobExecution = new JobExecution(5l);
		jobExecution.setJobInstance(jobInstance);
		StepExecution stepExecution = new StepExecution("masterFoo", jobExecution, 7l);
		stepExecution.getExecutionContext().put(JobLaunchingTasklet.XD_ORCHESTRATION_ID, "3");
		final StepContribution stepContribution = new StepContribution(stepExecution);
		final ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

		JobDefinition jobDefinition = new JobDefinition("foo", "foo");
		when(jobDefinitionRepository.findOne("foo")).thenReturn(jobDefinition);
		when(instanceRepository.findOne("foo")).thenReturn(jobDefinition);
		doReturn(true).when(launchingChannel).send(any(Message.class));

		FutureTask<RepeatStatus> result = new FutureTask<RepeatStatus>(new Callable<RepeatStatus>() {

			@Override
			public RepeatStatus call() throws Exception {
				return tasklet.execute(stepContribution, chunkContext);
			}
		});

		taskExecutor.execute(result);

		JobParameters jobParameters = new JobParametersBuilder().addString(JobLaunchingTasklet.XD_ORCHESTRATION_ID,
				"3").toJobParameters();
		JobExecution slaveExecution = new JobExecution(9l, jobParameters);
		slaveExecution.setStatus(BatchStatus.COMPLETED);
		slaveExecution.setEndTime(new Date());

		//This is to allow the job to "start" before mocking the sending of the event
		Thread.sleep(1000l);

		this.listeningChannel.send(MessageBuilder.withPayload(slaveExecution).build());

		assertEquals(result.get(5, TimeUnit.SECONDS), RepeatStatus.FINISHED);

		assertEquals(stepExecution.getExecutionContext().get(JobLaunchingTasklet.XD_ORCHESTRATION_ID), "3");

		verify(bus).bindPubSubConsumer(eq("tap:job:foo.job"), any(PublishSubscribeAmqpChannel.class),
				(Properties) isNull());
	}

	@Test
	public void testTimeout() throws Exception {
		this.tasklet = new JobLaunchingTasklet(bus, jobDefinitionRepository, instanceRepository, "foo", 2000l,
				launchingChannel, listeningChannel);
		JobInstance jobInstance = new JobInstance(3l, "masterFoo");
		JobExecution jobExecution = new JobExecution(5l);
		jobExecution.setJobInstance(jobInstance);
		StepExecution stepExecution = new StepExecution("masterFoo", jobExecution, 7l);
		final StepContribution stepContribution = new StepContribution(stepExecution);
		final ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

		JobDefinition jobDefinition = new JobDefinition("foo", "foo");
		when(jobDefinitionRepository.findOne("foo")).thenReturn(jobDefinition);
		when(instanceRepository.findOne("foo")).thenReturn(jobDefinition);
		doReturn(true).when(launchingChannel).send(any(Message.class));

		FutureTask<RepeatStatus> result = new FutureTask<RepeatStatus>(new Callable<RepeatStatus>() {

			@Override
			public RepeatStatus call() throws Exception {
				return tasklet.execute(stepContribution, chunkContext);
			}
		});

		taskExecutor.execute(result);

		try {
			result.get(5, TimeUnit.SECONDS);
			fail();
		}
		catch (ExecutionException e) {
			assertEquals(UnexpectedJobExecutionException.class, e.getCause().getClass());
			assertEquals("The job timed out while waiting for a result", e.getCause().getMessage());
		}
	}

}
