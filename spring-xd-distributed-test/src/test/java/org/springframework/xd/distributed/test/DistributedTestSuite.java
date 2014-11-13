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

package org.springframework.xd.distributed.test;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import org.springframework.xd.distributed.util.DefaultDistributedTestSupport;
import org.springframework.xd.distributed.util.DistributedTestSupport;

/**
 * Test suite for distributed tests. This test suite is responsible for
 * managing the lifecycle of the {@link DistributedTestSupport} class
 * used by all distributed tests. Any distributed tests that extend
 * {@link AbstractDistributedTests} must be added to the {@code @Suite.SuiteClasses}
 * to be included in the suite.
 *
 * @author Patrick Peralta
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	ContainerRedeploymentTests.class,
	JobStateTests.class,
	StreamDeploymentTests.class,
	StreamPartitionTests.class,
	StreamStateTests.class
})
public class DistributedTestSuite {

	/**
	 * Instance of {@link DistributedTestSupport} managed by this test suite
	 * and shared by tests that extend {@link AbstractDistributedTests}.
	 */
	public static DistributedTestSupport distributedTestSupport;

	/**
	 * Start the {@link DistributedTestSupport} used by test classes.
	 *
	 * @see DistributedTestSupport#startup
	 */
	@BeforeClass
	public static void beforeClass() {
		distributedTestSupport = new DefaultDistributedTestSupport();
		distributedTestSupport.startup();
	}

	/**
	 * Stop the {@link DistributedTestSupport} used by test classes.
	 *
	 * @see DistributedTestSupport#shutdownAll
	 */
	@AfterClass
	public static void afterClass() throws InterruptedException {
		distributedTestSupport.shutdownAll();
	}

}
