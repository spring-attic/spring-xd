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


package org.springframework.xd.shell.security;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.http.HttpStatus;
import org.springframework.shell.Bootstrap;
import org.springframework.shell.core.CommandResult;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.xd.dirt.server.singlenode.SingleNodeApplication;
import org.springframework.xd.shell.Configuration;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.test.RandomConfigurationSupport;

/**
 * Tests basic security support in the shell.
 *
 * @author Marius Bogoevici
 */
public class SecuredShellAccessTests {

	private static SingleNodeApplication singleNodeApplication;

	private static String originalConfigLocation;

	private static String adminPort;

	@BeforeClass
	public static void setUp() throws Exception {
		RandomConfigurationSupport randomConfigurationSupport = new RandomConfigurationSupport();
		originalConfigLocation = System.getProperty("spring.config.location");
		System.setProperty("spring.config.location",
				"classpath:org/springframework/xd/shell/security/securedServer.yml");
		singleNodeApplication = new SingleNodeApplication().run();
		adminPort = randomConfigurationSupport.getAdminServerPort();
	}

	@Test
	public void testShellWithCredentials() throws Exception {
		Bootstrap bootstrap = new Bootstrap();
		JLineShellComponent shell = bootstrap.getJLineShellComponent();
		CommandResult commandResult = shell.executeCommand(
				"admin config server --uri http://localhost:" + adminPort + " --username admin --password whosThere");
		assertThat(commandResult.isSuccess(), is(true));
		commandResult = shell.executeCommand("module list");
		assertThat(commandResult.isSuccess(), is(true));
		assertThat(commandResult.getResult(), instanceOf(Table.class));
		assertThat(((Table) commandResult.getResult()).getHeaders().size(), greaterThan(0));
	}

	@Test
	public void testShellWithPasswordFlag() throws Exception {
		try {
			// Simulate user input by replacing the system input stream
			SimulatedInputStream simulatedInputStream = new SimulatedInputStream("whosThere\n");
			System.setIn(simulatedInputStream);
			Bootstrap bootstrap = new Bootstrap();
			JLineShellComponent shell = bootstrap.getJLineShellComponent();
			assertThat(simulatedInputStream.isReadPerformed(), is(false));
			assertThat(simulatedInputStream.hasUnreadData(), is(true));
			CommandResult commandResult = shell.executeCommand(
					"admin config server --uri http://localhost:" + adminPort + " --username admin --password");
			assertThat(simulatedInputStream.isReadPerformed(), is(true));
			assertThat(simulatedInputStream.hasUnreadData(), is(false));
			assertThat(commandResult.isSuccess(), is(true));
			commandResult = shell.executeCommand("module list");
			assertThat(commandResult.isSuccess(), is(true));
			assertThat(commandResult.getResult(), instanceOf(Table.class));
			assertThat(((Table) commandResult.getResult()).getHeaders().size(), greaterThan(0));
		}
		finally {
			// restore the system input stream
			System.setIn(System.in);
		}
	}

	@Test
	public void testShellWithPasswordFlagFailsIfPasswordIsWrong() throws Exception {
		try {
			// Simulate user input by replacing the system input stream
			SimulatedInputStream simulatedInputStream = new SimulatedInputStream("aWrongPassword\n");
			System.setIn(simulatedInputStream);
			Bootstrap bootstrap = new Bootstrap();
			JLineShellComponent shell = bootstrap.getJLineShellComponent();
			assertThat(simulatedInputStream.isReadPerformed(), is(false));
			assertThat(simulatedInputStream.hasUnreadData(), is(true));
			CommandResult commandResult = shell.executeCommand(
					"admin config server --uri http://localhost:" + adminPort + " --username admin --password");
			assertThat(simulatedInputStream.isReadPerformed(), is(true));
			assertThat(simulatedInputStream.hasUnreadData(), is(false));
			assertThat(commandResult.isSuccess(), is(true));
			Configuration configuration = bootstrap.getApplicationContext().getBean(Configuration.class);
			assertThat(configuration.getTarget().getTargetException(), instanceOf(HttpClientErrorException.class));
			assertThat(((HttpClientErrorException) configuration.getTarget().getTargetException()).getStatusCode(),
					equalTo(HttpStatus.UNAUTHORIZED));
			commandResult = shell.executeCommand("module list");
			assertThat(commandResult.isSuccess(), is(false));
		}
		finally {
			// restore the system input stream
			System.setIn(System.in);
		}
	}

	@Test
	public void testShellNoPromptWithoutPasswordFlag() throws Exception {
		try {
			// Simulate user input by replacing the system input stream
			SimulatedInputStream simulatedInputStream = new SimulatedInputStream("whosThere\n");
			System.setIn(simulatedInputStream);
			Bootstrap bootstrap = new Bootstrap();
			JLineShellComponent shell = bootstrap.getJLineShellComponent();
			assertThat(simulatedInputStream.isReadPerformed(), is(false));
			assertThat(simulatedInputStream.hasUnreadData(), is(true));
			CommandResult commandResult = shell.executeCommand(
					"admin config server --uri http://localhost:" + adminPort + " --username admin");
			assertThat(simulatedInputStream.isReadPerformed(), is(false)); // without the --password flag, the shell doesn't prompt and doesn't read from input
			assertThat(simulatedInputStream.hasUnreadData(), is(true));
			assertThat(commandResult.isSuccess(), is(true));
			Configuration configuration = bootstrap.getApplicationContext().getBean(Configuration.class);
			assertThat(configuration.getTarget().getTargetException(), instanceOf(HttpClientErrorException.class));
			assertThat(((HttpClientErrorException) configuration.getTarget().getTargetException()).getStatusCode(),
					equalTo(HttpStatus.UNAUTHORIZED));
			commandResult = shell.executeCommand("module list");
			assertThat(commandResult.isSuccess(), is(false));
		}
		finally {
			// restore the system input stream
			System.setIn(System.in);
		}
	}

	@Test
	public void testShellWithWrongCredentials() throws Exception {
		Bootstrap bootstrap = new Bootstrap();
		JLineShellComponent shell = bootstrap.getJLineShellComponent();
		CommandResult commandResult = shell.executeCommand(
				"admin config server --uri http://localhost:" + adminPort + " --username admin --password whosThere2");
		assertThat(commandResult.isSuccess(), is(true));
		Configuration configuration = bootstrap.getApplicationContext().getBean(Configuration.class);
		assertThat(configuration.getTarget().getTargetException(), instanceOf(HttpClientErrorException.class));
		assertThat(((HttpClientErrorException) configuration.getTarget().getTargetException()).getStatusCode(),
				equalTo(HttpStatus.UNAUTHORIZED));
		commandResult = shell.executeCommand("module list");
		assertThat(commandResult.isSuccess(), is(false));
	}

	@Test
	public void testShellWithMissingUsername() throws Exception {
		Bootstrap bootstrap = new Bootstrap();
		JLineShellComponent shell = bootstrap.getJLineShellComponent();
		CommandResult commandResult = shell.executeCommand(
				"admin config server --uri http://localhost:" + adminPort + " --password whosThere");
		assertThat(commandResult.isSuccess(), is(true));
		assertThat(commandResult.getResult(),
				equalTo((Object) "A password may be specified only together with a username"));
		commandResult = shell.executeCommand("module list");
		assertThat(commandResult.isSuccess(), is(false));
	}

	@AfterClass
	public static void tearDown() throws Exception {
		singleNodeApplication.close();
		if (originalConfigLocation == null) {
			System.clearProperty("spring.config.location");
		}
		else {
			System.setProperty("spring.config.location", originalConfigLocation);
		}
	}

	private static class SimulatedInputStream extends InputStream {

		private ByteArrayInputStream content;

		private boolean readPerformed;

		public SimulatedInputStream(String content) {
			this.content = new ByteArrayInputStream(content.getBytes());
		}

		@Override
		public int read() throws IOException {
			readPerformed = true;
			return content.read();
		}

		public boolean isReadPerformed() {
			return readPerformed;
		}

		public boolean hasUnreadData() throws IOException {
			return content.available() > 0;
		}
	}
}
