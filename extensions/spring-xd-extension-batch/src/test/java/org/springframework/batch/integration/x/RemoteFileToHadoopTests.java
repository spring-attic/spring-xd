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

package org.springframework.batch.integration.x;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.data.hadoop.test.context.HadoopDelegatingSmartContextLoader;
import org.springframework.data.hadoop.test.context.MiniHadoopCluster;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.integration.bus.local.LocalMessageBus;
import org.springframework.xd.test.HostNotWindowsRule;


/**
 *
 * @author Gary Russell
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = HadoopDelegatingSmartContextLoader.class, classes = RemoteFileToHadoopTests.EmptyConfig.class)
@MiniHadoopCluster
public class RemoteFileToHadoopTests {

	@ClassRule
	public static HostNotWindowsRule hostNotWindowsRule = new HostNotWindowsRule();

	@Autowired
	private ApplicationContext context;

	@Autowired
	org.apache.hadoop.conf.Configuration configuration;

	private JobLauncher launcher;

	private Job job;

	private SessionFactory<?> sessionFactory;

	private MessageChannel requestsOut;

	private MessageChannel requestsIn;

	private MessageChannel repliesOut;

	private MessageChannel repliesIn;

	private MessageBus bus;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Before
	public void setup() throws Exception {
		byte[] bytes = "foobarbaz".getBytes();

		// using this trick here by getting hadoop minicluster from main test
		// context and then using it to override 'hadoopConfiguration' bean
		// which is imported from ftphdfs.xml.
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext();
		ctx.setConfigLocations("org/springframework/batch/integration/x/RemoteFileToHadoopTests-context.xml",
				"org/springframework/batch/integration/x/miniclusterconfig.xml");
		Properties properties = new Properties();
		properties.setProperty("restartable", "false");
		properties.setProperty("xd.config.home", "file:../../config");
		properties.setProperty("partitionResultsTimeout", "3600000");
		PropertiesPropertySource propertiesPropertySource = new PropertiesPropertySource("props", properties);
		ctx.getEnvironment().getPropertySources().addLast(propertiesPropertySource);
		ctx.setParent(context);
		ctx.refresh();

		this.sessionFactory = ctx.getBean(SessionFactory.class);

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

		this.launcher = ctx.getBean(JobLauncher.class);
		this.job = ctx.getBean(Job.class);
		this.requestsOut = ctx.getBean("stepExecutionRequests.output", MessageChannel.class);
		this.requestsIn = ctx.getBean("stepExecutionRequests.input", MessageChannel.class);
		this.repliesOut = ctx.getBean("stepExecutionReplies.output", MessageChannel.class);
		this.repliesIn = ctx.getBean("stepExecutionReplies.input", MessageChannel.class);

		this.bus = new LocalMessageBus();
		((LocalMessageBus) this.bus).setApplicationContext(ctx);
		this.bus.bindRequestor("foo", this.requestsOut, this.repliesIn, null);
		this.bus.bindReplier("foo", this.requestsIn, this.repliesOut, null);
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
		FileSystem fs = FileSystem.get(configuration);
		Path p1 = new Path("/qux/foo/bar.txt");
		fs.delete(p1, true);
		Path p2 = new Path("/qux/foo/baz.txt");
		fs.delete(p2, true);
		assertFalse(fs.exists(p1));
		assertFalse(fs.exists(p2));

		Map<String, JobParameter> params = new HashMap<String, JobParameter>();
		params.put("remoteDirectory", new JobParameter("/foo/"));
		params.put("hdfsDirectory", new JobParameter("/qux"));
		JobParameters parameters = new JobParameters(params);
		JobExecution execution = launcher.run(job, parameters);
		assertEquals(ExitStatus.COMPLETED, execution.getExitStatus());

		assertTrue(fs.exists(p1));
		assertTrue(fs.exists(p2));

		FSDataInputStream stream = fs.open(p1);
		byte[] out = new byte[9];
		stream.readFully(out);
		stream.close();
		assertEquals("foobarbaz", new String(out));

		stream = fs.open(p2);
		stream.readFully(out);
		stream.close();
		assertEquals("foobarbaz", new String(out));
	}

	@Configuration
	static class EmptyConfig {
	}

}
