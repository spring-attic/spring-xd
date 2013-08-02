/*
 * Copyright 2013 the original author or authors.
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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.springframework.shell.Bootstrap;
import org.springframework.shell.core.CommandResult;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.xd.dirt.server.AdminMain;
import org.springframework.xd.dirt.server.options.AdminOptions;
import org.springframework.xd.dirt.stream.StreamServer;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableRow;
import org.springframework.xd.test.redis.RedisAvailableRule;

/**
 * Superclass for performing integration tests of spring-xd shell commands.
 * 
 * JUnit's BeforeClass and AfterClass annotations are used to start and stop the
 * XDAdminServer in local mode with the default store configured to use
 * in-memory storage.
 * 
 * Note: This isn't ideal as it takes significant time to startup the embedded
 * XDContainer/tomcat and we should do this once across all tests.
 * 
 * @author Mark Pollack
 * @author Kashyap Parikh
 * 
 */
public abstract class AbstractShellIntegrationTest {

	@Rule
	public RedisAvailableRule redisAvailableRule = new RedisAvailableRule();
	
	private static final Log logger = LogFactory
			.getLog(AbstractShellIntegrationTest.class);

	private static StreamServer server;
	private static JLineShellComponent shell;

	@BeforeClass
	public static void startUp() throws InterruptedException, IOException {
		AdminOptions opts = AdminMain.parseOptions(new String[] { "--httpPort",
				"0", "--transport", "local", "--store", "redis",
				"--disableJmx", "true", "--analytics", "redis" });
		server = AdminMain.launchStreamServer(opts);
		Bootstrap bootstrap = new Bootstrap(new String[] { "--port",
				Integer.toString(server.getLocalPort()) });
		shell = bootstrap.getJLineShellComponent();
	}

	@AfterClass
	public static void shutdown() {
		logger.info("Stopping StreamServer");
		server.stop();

		logger.info("Stopping XD Shell");
		shell.stop();
	}

	public static JLineShellComponent getShell() {
		return shell;
	}
	
	public static StreamServer getStreamServer() {
		return server;
	}

	/**
	 * Execute a command and verify the command result.
	 */
	protected CommandResult executeCommand(String command) {
		CommandResult cr = getShell().executeCommand(command);
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		return cr;
	}
	
	/**
	 * Post data to http target
	 * @param target the http target
	 * @param data the data to send
	 */
	protected void httpPostData(String target, String data){
		executeCommand("http post --target "+target+" --data "+data);
	}
	
	/**
	 * Check if the counter exists 
	 * This method is temporary to verify if the counter is created when it received message
	 * @param counterName the counter name to check
	 */
	protected void checkIfCounterExists(String counterName) {
		Table t = (Table) getShell().executeCommand("counter list").getResult();
		assertTrue("Failure. Counter doesn't exist", 
				t.getRows().contains(new TableRow().addValue(1, counterName)));
	}
	
	/**
	 * Check the counter value using "counter display" shell command
	 * 
	 * @param counterName
	 * @param expectedCount
	 */
	protected void checkCounterValue(String counterName, String expectedCount) {
		CommandResult cr = executeCommand("counter display --name "
				+ counterName);
		assertEquals(expectedCount, cr.getResult());
	}

	/**
	 * Delete the counter with the given name
	 * 
	 * @param counterName
	 */
	protected void deleteCounter(String counterName) {
		CommandResult cr = executeCommand("counter delete --name "
				+ counterName);
		assertEquals("Deleted counter '" + counterName + "'", cr.getResult());
	}
}
