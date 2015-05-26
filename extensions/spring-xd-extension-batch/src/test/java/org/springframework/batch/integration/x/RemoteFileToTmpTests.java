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

package org.springframework.batch.integration.x;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.file.remote.InputStreamCallback;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.xd.dirt.integration.bus.BusTestUtils;
import org.springframework.xd.dirt.integration.bus.local.LocalMessageBus;


/**
 *
 * @author Gary Russell
 */
@ContextConfiguration
@TestPropertySource(properties = { "restartable = false", "partitionResultsTimeout = 3600000", "xd.config.home = file:../../config" })
@RunWith(SpringJUnit4ClassRunner.class)
public class RemoteFileToTmpTests {

	private static final String tmpDir = System.getProperty("java.io.tmpdir");

	@Autowired
	private JobLauncher launcher;

	@Autowired
	private Job job;

	@Autowired
	private SessionFactory<?> sessionFactory;

	@Autowired
	@Qualifier("stepExecutionRequests.output")
	private MessageChannel requestsOut;

	@Autowired
	@Qualifier("stepExecutionRequests.input")
	private MessageChannel requestsIn;

	@Autowired
	@Qualifier("stepExecutionReplies.output")
	private MessageChannel repliesOut;

	@Autowired
	@Qualifier("stepExecutionReplies.input")
	private MessageChannel repliesIn;

	private LocalMessageBus bus;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Before
	public void setup() throws Exception {
		byte[] bytes = "foo".getBytes();
		Session session = mock(Session.class);
		when(this.sessionFactory.getSession()).thenReturn(session);
		when(session.readRaw("/foo/bar.txt")).thenReturn(new ByteArrayInputStream(bytes));
		when(session.readRaw("/foo/baz.txt")).thenReturn(new ByteArrayInputStream(bytes));
		when(session.finalizeRaw()).thenReturn(true);
		Object[] fileList = new FTPFile[2];
		FTPFile file = new FTPFile();
		file.setName("bar.txt");
		fileList[0] = file;
		file = new FTPFile();
		file.setName("baz.txt");
		fileList[1] = file;
		when(session.list("/foo/")).thenReturn(fileList);

		this.bus = new LocalMessageBus();
		this.bus.setApplicationContext(BusTestUtils.MOCK_AC);
		this.bus.bindRequestor("foo", this.requestsOut, this.repliesIn, null);
		this.bus.bindReplier("foo", this.requestsIn, this.repliesOut, null);
		this.bus.afterPropertiesSet();
	}

	@After
	public void tearDown() {
		this.bus.unbindConsumer("foo", this.requestsOut);
		this.bus.unbindConsumer("foo", this.requestsIn);
		this.bus.unbindConsumer("foo", this.repliesIn);
		this.bus.unbindConsumer("foo", this.repliesOut);
	}

	@Test
	public void testSimple() throws Exception {
		Map<String, JobParameter> params = new HashMap<String, JobParameter>();
		params.put("remoteDirectory", new JobParameter("/foo/"));
		params.put("hdfsDirectory", new JobParameter("/qux"));
		JobParameters parameters = new JobParameters(params);
		JobExecution execution = launcher.run(job, parameters);
		assertEquals(ExitStatus.COMPLETED, execution.getExitStatus());

		File file = new File(tmpDir, "_foo_bar.txt");
		assertTrue(file.exists());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		FileCopyUtils.copy(new FileInputStream(file), baos);
		assertEquals("foo", new String(baos.toByteArray()));
		file.delete();

		file = new File(tmpDir, "_foo_baz.txt");
		assertTrue(file.exists());
		baos = new ByteArrayOutputStream();
		FileCopyUtils.copy(new FileInputStream(file), baos);
		assertEquals("foo", new String(baos.toByteArray()));
		file.delete();
	}

	/**
	 * Replaces Hadoop tasklet so we can just write to java.io.tmpdir instead.
	 */
	public static class RemoteFileToTmpDirTasklet implements Tasklet {

		private final Logger logger = LoggerFactory.getLogger(this.getClass());

		private final RemoteFileTemplate<?> template;

		@SuppressWarnings("rawtypes")
		public RemoteFileToTmpDirTasklet(RemoteFileTemplate template) {
			template.setFileNameExpression(new SpelExpressionParser().parseExpression("payload"));
			try {
				template.afterPropertiesSet();
			}
			catch (Exception e) {
				logger.error("Failed to initialize RemoteFileTemplate", e);
			}
			this.template = template;
		}

		@Override
		public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
			final String fileName = chunkContext.getStepContext().getStepExecution().getExecutionContext().getString(
					"filePath");
			Assert.notNull(fileName);
			boolean result = this.template.get(MessageBuilder.withPayload(fileName).build(), new InputStreamCallback() {

				@Override
				public void doWithInputStream(InputStream stream) throws IOException {
					// TODO hadoop
					FileOutputStream out = new FileOutputStream(new File(tmpDir,
							fileName.replaceAll("\\/", "_")));
					FileCopyUtils.copy(stream, out);
				}

			});
			if (!result) {
				throw new MessagingException("Error during file transfer");
			}
			else {
				return RepeatStatus.FINISHED;
			}
		}

	}

}
