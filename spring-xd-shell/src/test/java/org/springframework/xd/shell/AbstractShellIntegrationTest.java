/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.shell;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;

import org.springframework.messaging.SubscribableChannel;
import org.springframework.shell.Bootstrap;
import org.springframework.shell.core.CommandResult;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.IdGenerator;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.module.ArchiveModuleRegistry;
import org.springframework.xd.dirt.module.DelegatingModuleRegistry;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.server.SingleNodeApplication;
import org.springframework.xd.dirt.test.SingleNodeIntegrationTestSupport;
import org.springframework.xd.test.RandomConfigurationSupport;
import org.springframework.xd.test.redis.RedisTestSupport;

/**
 * Superclass for performing integration tests of spring-xd shell commands.
 *
 * JUnit's BeforeClass and AfterClass annotations are used to start and stop the XDAdminServer in local mode with the
 * default store configured to use in-memory storage.
 *
 * Note: This isn't ideal as it takes significant time to startup the embedded XDContainer/tomcat and we should do this
 * once across all tests.
 *
 * @author Mark Pollack
 * @author Kashyap Parikh
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 */
public abstract class AbstractShellIntegrationTest {

	public static boolean SHUTDOWN_AFTER_RUN = true;

	private final IdGenerator idGenerator = new AlternativeJdkIdGenerator();

	@ClassRule
	public static RedisTestSupport redisAvailableRule = new RedisTestSupport();

	private static final Log logger = LogFactory.getLog(AbstractShellIntegrationTest.class);

	private static SingleNodeApplication application;

	private static JLineShellComponent shell;

	private Set<File> toBeDeleted = new HashSet<File>();

	protected static SingleNodeIntegrationTestSupport integrationTestSupport;

	private Random random = new Random();

	/**
	 * Used to capture currently executing test method.
	 */
	@Rule
	public TestName name = new TestName();

	@BeforeClass
	public static synchronized void startUp() throws InterruptedException, IOException {
		RandomConfigurationSupport randomConfigSupport = new RandomConfigurationSupport();
		if (application == null) {
			application = new SingleNodeApplication().run("--transport", "local", "--analytics", "redis");
			integrationTestSupport = new SingleNodeIntegrationTestSupport(application, "classpath:/spring-xd/xd/modules");
			Bootstrap bootstrap = new Bootstrap(new String[] { "--port", randomConfigSupport.getAdminServerPort() });
			shell = bootstrap.getJLineShellComponent();
		}
		if (!shell.isRunning()) {
			shell.start();
		}
	}

	@AfterClass
	public static void shutdown() {
		if (SHUTDOWN_AFTER_RUN) {
			logger.info("Stopping XD Shell");
			shell.stop();
			if (application != null) {
				logger.info("Stopping Single Node Server");
				application.close();
				redisAvailableRule.getResource().destroy();
			}
		}
	}

	public static JLineShellComponent getShell() {
		return shell;
	}

	protected MessageBus getMessageBus() {
		return integrationTestSupport.messageBus();
	}

	protected SubscribableChannel getErrorChannel() {
		return application.containerContext().getBean("errorChannel", SubscribableChannel.class);
	}

	private String generateUniqueName(String name) {
		return name + "-" + idGenerator.generateId();
	}

	private String generateUniqueName() {
		return generateUniqueName(name.getMethodName().replace('[', '-').replaceAll("]", ""));
	}

	protected String generateStreamName(String name) {
		return (name == null) ? generateUniqueName() : generateUniqueName(name);
	}

	protected String generateStreamName() {
		return generateStreamName(null);
	}

	protected String generateQueueName() {
		StackTraceElement[] element = Thread.currentThread().getStackTrace();
		// Assumption here is that generateQueueName() is called from the @Test method
		return "queue:" + element[2].getMethodName() + random.nextInt();
	}

	protected String getTapName(String streamName) {
		return "tap:stream:" + streamName;
	}

	protected String generateJobName(String name) {
		return (name == null) ? generateUniqueName() : generateUniqueName(name);
	}

	protected String generateJobName() {
		return generateJobName(null);
	}

	protected String getJobLaunchQueue(String jobName) {
		return "queue:job:" + jobName;
	}

	/**
	 * Execute a command and verify the command result.
	 */
	protected CommandResult executeCommand(String command) {
		CommandResult cr = getShell().executeCommand(command);
		if (cr.getException() != null) {
			cr.getException().printStackTrace();
		}
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		return cr;
	}

	protected CommandResult executeCommandExpectingFailure(String command) {
		CommandResult cr = getShell().executeCommand(command);
		assertFalse("Expected command to fail.  CommandResult = " + cr.toString(), cr.isSuccess());
		return cr;
	}

}

