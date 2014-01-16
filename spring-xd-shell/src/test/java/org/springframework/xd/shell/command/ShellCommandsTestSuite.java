/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.shell.command;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import org.springframework.xd.shell.AbstractShellIntegrationTest;


/**
 * 
 * @author David Turanski
 */
@RunWith(Suite.class)
@SuiteClasses({
	DateCommandTest.class,
	FileSourceAndFileSinkTests.class,
	HttpCommandTests.class,
	JobCommandTests.class,
	// MailCommandTests.class,
	MessageStoreBenchmark.class,
	MetricsTests.class,
	ModuleClasspathTests.class,
	ModuleCommandTests.class,
	NamedChannelTests.class,
	ProcessorsTests.class,
	RuntimeCommandTests.class,
	SpelPropertyAccessorIntegrationTests.class,
	StreamCommandTests.class,
	TcpModulesTests.class,
	TriggerModulesTest.class
})
public class ShellCommandsTestSuite {

	@BeforeClass
	public static void startUp() throws InterruptedException, IOException {
		AbstractShellIntegrationTest.startUp();
	}

	@AfterClass
	public static void shutdown() {
		AbstractShellIntegrationTest.shutdown();
	}

}
