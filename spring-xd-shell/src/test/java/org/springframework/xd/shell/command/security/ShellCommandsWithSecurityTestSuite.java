/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.shell.command.security;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import org.springframework.xd.shell.AbstractShellIntegrationTest;
import org.springframework.xd.test.PackageSuiteRunner;
import org.springframework.xd.test.redis.RedisTestSupport;

/**
 * Runs all the Shell Integration Tests with security enabled starting XD once.
 *
 * @author Gunnar Hillert
 */
@RunWith(PackageSuiteRunner.class)
public class ShellCommandsWithSecurityTestSuite {

	@ClassRule
	public static RedisTestSupport redisAvailableRule = new RedisTestSupport();

	@BeforeClass
	public static void startUp() throws InterruptedException, IOException {
		AbstractShellIntegrationTest.startupWithSecurityAndFullPermissions();
		AbstractShellIntegrationTest.SHUTDOWN_AFTER_RUN = false;
	}

	@AfterClass
	public static void shutdown() {
		AbstractShellIntegrationTest.SHUTDOWN_AFTER_RUN = true;
		AbstractShellIntegrationTest.doShutdown();
	}

}
