/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.integration.test;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.batch.core.BatchStatus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Test that a Batch Simple job module can be installed on XD and is recognized after XD has started.
 *
 * @author Glenn Renfro
 */
public class SimpleJobTest extends AbstractJobTest {


	private final static String JOB_NAME = "myjob";

	@Rule
	public SimpleJobTestSupport jobAvailable = new SimpleJobTestSupport();

	@Before
	public void initialize() {
		List<File> jars = new ArrayList<>();
		jars.add(new File("springxd-batch-simple-1.0.0.BUILD-SNAPSHOT.jar"));
		List<File> configFiles = new ArrayList<>();
		configFiles.add(new File("myjob.xml"));
		copyJobToCluster(JOB_NAME, jars, configFiles);
	}

	/**
	 * Verifies that batch simple (myjob) can be deployed and executed.
	 */
	@Test
	public void testSimpleJob() {
		String jobName = "sj" + UUID.randomUUID().toString();
		job(jobName, JOB_NAME, true);
		jobLaunch(jobName);
		waitForXD();
		assertEquals(BatchStatus.COMPLETED, getJobExecutionStatus(jobName));
	}

	private class SimpleJobTestSupport implements TestRule {

		@Override
		public Statement apply(Statement base, Description description) {
			boolean isSatisfied = new File("springxd-batch-simple-1.0.0.BUILD-SNAPSHOT.jar").exists() &&
					new File("myjob.xml").exists();
			if (!isSatisfied) {
				return new Statement() {
					@Override
					public void evaluate() {
						Assume.assumeTrue("Skipping test due to SimpleJob config and jar not being available", false);
					}
				};
			}
			return base;
		}
	}
}
