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


package org.springframework.xd.shell.security;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.http.HttpStatus;
import org.springframework.shell.Bootstrap;
import org.springframework.shell.core.CommandResult;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.xd.dirt.server.SingleNodeApplication;
import org.springframework.xd.shell.Configuration;
import org.springframework.xd.shell.util.Table;

/**
 * Tests basic security support in the shell.
 *
 * @author Marius Bogoevici
 */
public class SecuredShellAccessTest {

	private static SingleNodeApplication singleNodeApplication;

	private static String originalConfigLocation;

	private static String adminPort;

	@BeforeClass
	public static void setUp() throws Exception {
		originalConfigLocation = System.getProperty("spring.config.location");
		System.setProperty("spring.config.location", "classpath:org/springframework/xd/shell/security/securedServer.yml");
		singleNodeApplication = new SingleNodeApplication().run();
		adminPort = singleNodeApplication.adminContext().getEnvironment().resolvePlaceholders("${server.port}");
	}

	@Test
	public void testShellWithCredentials() throws Exception {
		Bootstrap bootstrap = new Bootstrap();
		JLineShellComponent shell = bootstrap.getJLineShellComponent();
		CommandResult commandResult = shell.executeCommand("admin config server --uri http://localhost:" + adminPort + " --username admin --password whosThere");
		assertThat(commandResult.isSuccess(), is(true));
		commandResult = shell.executeCommand("module list");
		assertThat(commandResult.isSuccess(), is(true));
		assertThat(commandResult.getResult(), instanceOf(Table.class));
		assertThat(((Table) commandResult.getResult()).getHeaders().size(), greaterThan(0));

	}

	@Test
	public void testShellWithWrongCredentials() throws Exception {
		Bootstrap bootstrap = new Bootstrap();
		JLineShellComponent shell = bootstrap.getJLineShellComponent();
		CommandResult commandResult = shell.executeCommand("admin config server --uri http://localhost:" + adminPort + " --username admin --password whosThere2");
		assertThat(commandResult.isSuccess(), is(true));
		Configuration configuration = bootstrap.getApplicationContext().getBean(Configuration.class);
		assertThat(configuration.getTarget().getTargetException(), instanceOf(HttpClientErrorException.class));
		assertThat(((HttpClientErrorException) configuration.getTarget().getTargetException()).getStatusCode(), equalTo(HttpStatus.UNAUTHORIZED));
		commandResult = shell.executeCommand("module list");
		assertThat(commandResult.isSuccess(), is(false));
	}

	@Test
	public void testShellWithMissingUsername() throws Exception {
		Bootstrap bootstrap = new Bootstrap();
		JLineShellComponent shell = bootstrap.getJLineShellComponent();
		CommandResult commandResult = shell.executeCommand("admin config server --uri http://localhost:" + adminPort + " --password whosThere");
		assertThat(commandResult.isSuccess(), is(true));
		Configuration configuration = bootstrap.getApplicationContext().getBean(Configuration.class);
		assertThat(configuration.getTarget().getTargetException(), instanceOf(IllegalArgumentException.class));
		assertThat(configuration.getTarget().getTargetException().getMessage(), equalTo("A password may be specified only together with a user name"));
		commandResult = shell.executeCommand("module list");
		assertThat(commandResult.isSuccess(), is(false));
	}

	@AfterClass
	public static void tearDown() throws Exception {
		singleNodeApplication.close();
		if (originalConfigLocation == null) {
			System.clearProperty("spring.config.location");
		} else {
			System.setProperty("spring.config.location", originalConfigLocation);
		}
	}
}
