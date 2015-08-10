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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;

import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.rest.client.impl.SpringXDTemplate;
import org.springframework.xd.test.fixtures.EventuallyMatcher;

/**
 * Series of tests to validate job deployments and job state transitions.
 * This test uses the {@code timestampfile} job.
 *
 * @author Patrick Peralta
 */
public class JobStateTests extends AbstractDistributedTests {

	/**
	 * Output directory for {@code timestampfile} job.
	 */
	private final static String DIRECTORY = System.getProperty("java.io.tmpdir");

	/**
	 * Extension for {@code timestampfile} job output file.
	 */
	private static final String FILE_EXTENSION = "txt";

	/**
	 * Date format for {@code timestampfile} job output file.
	 */
	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	/**
	 * Assert the correct deployment and state transition for job deployments,
	 * in particular with regard to container availability.
	 *
	 * @throws Exception
	 */
	@Test
	public void testJobStateTransition() throws Exception {
		SpringXDTemplate template = ensureTemplate();
		String jobName = testName.getMethodName() + "-ticktock";

		String definition = String.format("timestampfile --directory=%s --format='%s' --fileExtension=%s",
				DIRECTORY, DATE_FORMAT, FILE_EXTENSION);

		String fileName = DIRECTORY + File.separatorChar + jobName + '.' + FILE_EXTENSION;

		try {
			template.jobOperations().createJob(jobName, definition, false);
			verifyJobCreated(jobName);
			verifyJobState(jobName, DeploymentUnitStatus.State.undeployed);

			template.jobOperations().deploy(jobName, Collections.<String, String>emptyMap());
			verifyJobState(jobName, DeploymentUnitStatus.State.failed);

			startContainer();
			Map<Long, String> mapPidUuid = waitForContainers();
			assertEquals(1, mapPidUuid.size());

			verifyJobState(jobName, DeploymentUnitStatus.State.deployed);
			template.jobOperations().launchJob(jobName, null);
			assertThat(fileName, fileUpdated(System.currentTimeMillis()));

			shutdownContainer(mapPidUuid.keySet().iterator().next());
			waitForContainers();
			verifyJobState(jobName, DeploymentUnitStatus.State.failed);

			startContainer();
			waitForContainers();
			verifyJobState(jobName, DeploymentUnitStatus.State.deployed);
		}
		finally {
			File file = new File(fileName);
			if (file.exists()) {
				if (!file.delete()) {
					logger.warn("Could not delete job output file {}", fileName);
				}
			}
		}
	}

	/**
	 * Wrap a {@link FileContentMatcher} in an {@link EventuallyMatcher}.
	 *
	 * @param startTime the approximate start time of the test
	 * @return matcher for the expected file
	 */
	private EventuallyMatcher<String> fileUpdated(long startTime) {
		return new EventuallyMatcher<String>(new FileContentMatcher(startTime));
	}


	/**
	 * Custom matcher for the {@code timestampfile} job output file.
	 * This matcher performs the following checks:
	 * <ul>
	 *     <li>The expected file was created</li>
	 *     <li>The file contains a timestamp as its last line</li>
	 *     <li>The timestamp indicates a time not less than 60
	 *         seconds after the test was launched; this handles
	 *         the scenario where a prior test run correctly
	 *         executed the job but the current test run did not</li>
	 * </ul>
	 */
	private class FileContentMatcher extends BaseMatcher<String> {

		/**
		 * Approximate start time for the running test.
		 */
		private final long startTime;

		/**
		 * Description of mismatch; i.e. a detailed description
		 * of failed assertions.
		 */
		private final StringBuilder mismatch = new StringBuilder();

		/**
		 * Formatter for dates in the job output file.
		 */
		private final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

		/**
		 * Construct a {@code FileContentMatcher} that expects a timestamp
		 * in the job output file.
		 *
		 * @param startTime the approximate start time of the test
		 */
		private FileContentMatcher(long startTime) {
			this.startTime = startTime;
		}

		/**
		 * Return {@code true} if:
		 * <ul>
		 *     <li>The expected file was created</li>
		 *     <li>The file contains a timestamp as its last line</li>
		 *     <li>The timestamp indicates a time not less than 60
		 *         seconds after the test was launched; this handles
		 *         the scenario where a prior test run correctly
		 *         executed the job but the current test run did not</li>
		 * </ul>
		 *
		 * @param item name of the timestamp file that should be created by the job
		 * @return true if the file contains an acceptable timestamp
		 */
		@Override
		public boolean matches(Object item) {
			String fileName = (String) item;
			File file = new File(fileName);
			if (!file.exists()) {
				mismatch.append("file ").append(fileName).append(" not found");
				return false;
			}

			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file));
				String lastLine = null;
				String line;

				do {
					line = reader.readLine();
					if (line != null) {
						lastLine = line;
					}
				}
				while (line != null);

				if (lastLine == null) {
					mismatch.append("file ").append(fileName).append(" is empty");
					return false;
				}

				long fileTime = dateFormat.parse(lastLine).getTime();
				if (fileTime - startTime > 60000) {
					mismatch.append("file timestamp was written more than one minute after start of test: ")
							.append(" file timestamp: ")
							.append(lastLine)
							.append(", start of test: ")
							.append(dateFormat.format(new Date(startTime)));
					return false;
				}
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			finally {
				if (reader != null) {
					try {
						reader.close();
					}
					catch (IOException e) {
						// ignore errors on close
					}
				}
			}

			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void describeMismatch(Object item, Description description) {
			description.appendText(mismatch.toString());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void describeTo(Description description) {
			description.appendText("expected timestamp file");
		}
	}

}
