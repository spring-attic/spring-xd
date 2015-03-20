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
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.hateoas.PagedResources;
import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.distributed.util.DefaultDistributedTestSupport;
import org.springframework.xd.distributed.util.DistributedTestSupport;
import org.springframework.xd.rest.client.impl.SpringXDTemplate;
import org.springframework.xd.rest.domain.JobDefinitionResource;
import org.springframework.xd.rest.domain.ModuleMetadataResource;
import org.springframework.xd.rest.domain.StreamDefinitionResource;

import com.oracle.tools.runtime.java.JavaApplication;
import com.oracle.tools.runtime.java.SimpleJavaApplication;

/**
 * Base class for distributed tests. Implementations of this class
 * <em>must</em> have their class names present in the {@link DistributedTestSuite}
 * {@code @Suite.SuiteClasses} annotation in order to be included in the
 * test suite executed by the build.
 * <p/>
 * Test implementations may assume that the minimum infrastructure
 * for an XD distributed system has been started up (admin server, ZooKeeper,
 * HSQL). Containers can be started by invoking {@link #startContainer}. Commands
 * may be invoked against the admin via the {@link SpringXDTemplate} returned
 * by {@link #ensureTemplate}.
 * <p/>
 * All containers will be shut down and all streams destroyed after each
 * test execution. The rest of the infrastructure will continue running
 * until the test suite has completed execution.
 *
 * @author Patrick Peralta
 */
public abstract class AbstractDistributedTests implements DistributedTestSupport {

	/**
	 * Logger.
	 */
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Maximum number of milliseconds a thread will be blocked
	 * while waiting for a module/job/stream state transition.
	 */
	private static final long STATE_TRANSITION_TIMEOUT = 60000;

	/**
	 * Distributed test support infrastructure.
	 */
	private static DistributedTestSupport distributedTestSupport;

	/**
	 * If true, this test is executing in the test suite {@link DistributedTestSuite}.
	 * This also means that the test suite will manage the lifecycle of
	 * {@link #distributedTestSupport}. If false, the instance of this test will
	 * manage the lifecycle.
	 */
	private static boolean suiteDetected;

	/**
	 * Name of currently executing unit test.
	 */
	@Rule
	public TestName testName = new TestName();


	/**
	 * Constructor for {@link AbstractDistributedTests}. If this test
	 * is executing as part of test suite {@link DistributedTestSuite},
	 * the {@link #distributedTestSupport} is obtained from the test suite.
	 * This also means that the lifecycle for the test support object
	 * is managed by the test suite.
	 */
	protected AbstractDistributedTests() {
		if (DistributedTestSuite.distributedTestSupport != null) {
			distributedTestSupport = DistributedTestSuite.distributedTestSupport;
			suiteDetected = true;
		}
	}

	/**
	 * Before test execution, ensure that an instance of
	 * {@link #distributedTestSupport} has been created. If the reference
	 * is null, a new instance is created. If a new instance is created,
	 * this test will manage the lifecycle for the test support object.
	 */
	@Before
	public void before() throws Exception {
		if (distributedTestSupport == null) {
			distributedTestSupport = new DefaultDistributedTestSupport();
			distributedTestSupport.startup();
			suiteDetected = false;
		}
	}

	/**
	 * After each test execution, all containers are shut down
	 * and all streams/jobs destroyed.
	 */
	@After
	public void after() throws InterruptedException {
		distributedTestSupport.ensureTemplate().streamOperations().destroyAll();
		distributedTestSupport.ensureTemplate().jobOperations().destroyAll();
		distributedTestSupport.shutdownContainers();
	}

	/**
	 * After all tests are executed, {@link DistributedTestSupport#shutdownAll}
	 * is invoked if an instance was not provided by the test suite
	 * {@link DistributedTestSuite}.
	 */
	@AfterClass
	public static void afterClass() throws InterruptedException {
		if (!suiteDetected) {
			distributedTestSupport.shutdownAll();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startup() {
		distributedTestSupport.startup();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SpringXDTemplate ensureTemplate() {
		return distributedTestSupport.ensureTemplate();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JavaApplication<SimpleJavaApplication> startContainer(Properties properties) {
		return distributedTestSupport.startContainer(properties);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JavaApplication<SimpleJavaApplication> startContainer() {
		return distributedTestSupport.startContainer();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<Long, String> waitForContainers() throws InterruptedException {
		return distributedTestSupport.waitForContainers();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdownContainer(long pid) {
		distributedTestSupport.shutdownContainer(pid);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdownContainers() throws InterruptedException {
		distributedTestSupport.shutdownContainers();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdownAll() throws InterruptedException {
		distributedTestSupport.shutdownAll();
	}

	/**
	 * Return a mapping of runtime modules to containers for a
	 * given stream name. Note that this method assumes that the
	 * stream has been deployed. To ensure that the stream has been
	 * deployed, invoke {@link #verifyStreamDeployed} prior to invoking
	 * this method.
	 *
	 * @return mapping of modules to the containers they are deployed to
	 * @see #verifyStreamDeployed
	 */
	protected ModuleRuntimeContainers retrieveModuleRuntimeContainers(String streamName) {
		ModuleRuntimeContainers containers = new ModuleRuntimeContainers();
		for (ModuleMetadataResource module : distributedTestSupport.ensureTemplate()
				.runtimeOperations().listDeployedModules()) {
			String moduleStreamName = module.getUnitName();
			String[] fields = module.getName().split("\\.");
			String moduleLabel = fields[0];

			if (moduleStreamName.equals(streamName)) {
				switch (module.getModuleType()) {
					case source:
						containers.addSourceContainer(module.getContainerId());
						break;
					case sink:
						containers.addSinkContainer(module.getContainerId());
						break;
					case processor:
						containers.addProcessorContainer(module.getContainerId(), moduleLabel);
				}
			}
		}

		assertTrue(String.format("A source and/or sink module is missing from %s", containers),
				containers.isComplete());
		return containers;
	}

	/**
	 * Assert that the given stream has been created.
	 *
	 * @param streamName  name of stream to verify
	 */
	protected void verifyStreamCreated(String streamName) throws InterruptedException {
		long expiry = System.currentTimeMillis() + STATE_TRANSITION_TIMEOUT;
		while (!streamExists(streamName) && System.currentTimeMillis() < expiry) {
			Thread.sleep(500);
		}
		assertTrue(streamExists(streamName));
	}

	/**
	 * Check if the stream exists.
	 *
	 * @param streamName stream name
	 * @return boolean true if the stream exists.
	 */
	private boolean streamExists(String streamName) {
		PagedResources<StreamDefinitionResource> list = distributedTestSupport.ensureTemplate()
				.streamOperations().list();

		for (StreamDefinitionResource stream : list) {
			if (stream.getName().equals(streamName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Assert that the given job has been created.
	 *
	 * @param jobName  name of job to verify
	 */
	protected void verifyJobCreated(String jobName) throws InterruptedException {
		long expiry = System.currentTimeMillis() + STATE_TRANSITION_TIMEOUT;
		while (!streamExists(jobName) && System.currentTimeMillis() < expiry) {
			Thread.sleep(500);
		}
		assertTrue(jobExists(jobName));
	}

	/**
	 * Check if the job exists.
	 *
	 * @param jobName job name
	 * @return boolean true if the stream exists.
	 */
	private boolean jobExists(String jobName) {
		PagedResources<JobDefinitionResource> list = distributedTestSupport.ensureTemplate()
				.jobOperations().list();

		for (JobDefinitionResource job : list) {
			if (job.getName().equals(jobName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return a {@link JobDefinitionResource} with a name matching
	 * the requested {@code jobName}.
	 *
	 * @param jobName name of job
	 *
	 * @return a {@code JobDefinitionResource} for the job or {@code null}
	 *         if the job does not exist
	 */
	private JobDefinitionResource getJob(String jobName) {
		PagedResources<JobDefinitionResource> list = distributedTestSupport.ensureTemplate()
				.jobOperations().list();

		for (JobDefinitionResource job : list) {
			if (job.getName().equals(jobName)) {
				return job;
			}
		}

		return null;
	}

	/**
	 * Block the executing thread until either the stream state is
	 * {@link DeploymentUnitStatus.State#deployed} or 30 seconds
	 * have elapsed.
	 *
	 * @param streamName name of stream to verify
	 * @throws InterruptedException
	 */
	protected void verifyStreamDeployed(String streamName) throws InterruptedException {
		verifyStreamState(streamName, DeploymentUnitStatus.State.deployed);
	}

	/**
	 * Block the executing thread until either the stream state
	 * matches the indicated state or 30 seconds have elapsed.
	 *
	 * @param streamName name of stream to verify
	 * @param expected   the expected state of the stream
	 * @throws InterruptedException
	 * @see #STATE_TRANSITION_TIMEOUT
	 */
	protected void verifyStreamState(String streamName, DeploymentUnitStatus.State expected)
			throws InterruptedException {
		long expiry = System.currentTimeMillis() + STATE_TRANSITION_TIMEOUT;
		DeploymentUnitStatus.State state = null;

		while (state != expected && System.currentTimeMillis() < expiry) {
			Thread.sleep(500);
			state = getStreamState(streamName);
		}

		logger.debug("Stream '{}' state: {}", streamName, state);
		assertEquals("Failed assertion for stream " + streamName, expected, state);
	}

	/**
	 * Block the executing thread until either the job state
	 * matches the indicated state or 30 seconds have elapsed.
	 *
	 * @param jobName name of job to verify
	 * @param expected   the expected state of the job
	 * @throws InterruptedException
	 * @see #STATE_TRANSITION_TIMEOUT
	 */
	protected void verifyJobState(String jobName, DeploymentUnitStatus.State expected)
			throws InterruptedException {
		long expiry = System.currentTimeMillis() + STATE_TRANSITION_TIMEOUT;
		DeploymentUnitStatus.State state = null;

		while (state != expected && System.currentTimeMillis() < expiry) {
			Thread.sleep(500);
			state = getJobState(jobName);
		}

		logger.debug("Job '{}' state: {}", jobName, state);
		assertEquals("Failed assertion for job " + jobName, expected, state);
	}

	/**
	 * Return the state of the given stream.
	 *
	 * @param streamName name of stream for which to obtain state
	 * @return the state of the stream
	 */
	protected DeploymentUnitStatus.State getStreamState(String streamName) {
		PagedResources<StreamDefinitionResource> list = distributedTestSupport.ensureTemplate()
				.streamOperations().list();

		for (StreamDefinitionResource stream : list) {
			if (stream.getName().equals(streamName)) {
				return DeploymentUnitStatus.State.valueOf(stream.getStatus());
			}
		}

		throw new IllegalStateException(String.format("Stream %s not deployed", streamName));
	}

	/**
	 * Return the state of the given job.
	 *
	 * @param jobName name of job for which to obtain state
	 * @return the state of the job
	 */
	protected DeploymentUnitStatus.State getJobState(String jobName) {
		JobDefinitionResource job = getJob(jobName);
		if (job == null) {
			throw new IllegalStateException(String.format("Job %s not deployed", jobName));
		}
		return DeploymentUnitStatus.State.valueOf(job.getStatus());
	}


	/**
	 * Mapping of source and sink modules to the containers they are
	 * deployed to.
	 */
	protected static class ModuleRuntimeContainers {

		/**
		 * IDs of containers that have deployed a source module.
		 */
		private final Collection<String> sourceContainers = new HashSet<String>();

		/**
		 * IDs of containers that have deployed a sink module.
		 */
		private final Collection<String> sinkContainers = new HashSet<String>();

		/**
		 * Map of container IDs to a set of processor module labels that
		 * have been deployed by the container.
		 */
		private final Map<String, Set<String>> processorContainers = new HashMap<String, Set<String>>();

		/**
		 * @see #sourceContainers
		 */
		public Collection<String> getSourceContainers() {
			return sourceContainers;
		}

		/**
		 * @see #sourceContainers
		 */
		public void addSourceContainer(String sourceContainer) {
			this.sourceContainers.add(sourceContainer);
		}

		/**
		 * @see #processorContainers
		 */
		public Map<String, Set<String>> getProcessorContainers() {
			return processorContainers;
		}

		/**
		 * @see #processorContainers
		 */
		public void addProcessorContainer(String container, String moduleDescription) {
			Set<String> labels = processorContainers.get(container);
			if (labels == null) {
				labels = new HashSet<String>();
				processorContainers.put(container, labels);
			}
			labels.add(moduleDescription);
		}

		/**
		 * @see #sinkContainers
		 */
		public Collection<String> getSinkContainers() {
			return sinkContainers;
		}

		/**
		 * @see #sinkContainers
		 */
		public void addSinkContainer(String sinkContainer) {
			this.sinkContainers.add(sinkContainer);
		}

		/**
		 * Return true if a source and sink container have been
		 * populated.
		 *
		 * @return true if source and sink containers have been populated
		 */
		public boolean isComplete() {
			return (!sourceContainers.isEmpty() && !sinkContainers.isEmpty());
		}

		@Override
		public String toString() {
			return "ModuleRuntimeContainers{" +
					"sourceContainers=" + sourceContainers +
					", sinkContainers=" + sinkContainers +
					", processorContainers=" + processorContainers +
					'}';
		}
	}

}
