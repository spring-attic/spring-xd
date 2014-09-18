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

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Test that a Batch Basic job module can be installed on XD and is recognized after XD has started.
 *
 * @author Glenn Renfro
 */
public class BasicJobTest extends AbstractJobTest {

	private final static String JOB_NAME = "batchbasic";

	@Before
	public void initialize() {
		List<File> jars = new ArrayList<>();
		List<File> configFiles = new ArrayList<>();
		configFiles.add(new File("config/batchbasic.xml"));
		copyJobToCluster(JOB_NAME, jars, configFiles);
	}

	/**
	 * Verifies that batch basic (batchbasic) can be deployed and executed.
	 */
	@Test
	public void testBasicJob() {
		String jobName = "bb" + UUID.randomUUID().toString();
		job(jobName, JOB_NAME,true);
		setupDataFiles(getContainerHostForJob(jobName), "/tmp", "sample.txt", "h,e,l,l,o,w,o,r,l,d");
		jobLaunch(jobName);
		waitForXD();
		assertEquals(BatchStatus.COMPLETED, getJobExecutionStatus(jobName));
	}

}
