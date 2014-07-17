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

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import com.oracle.tools.runtime.java.JavaApplication;
import com.oracle.tools.runtime.java.SimpleJavaApplication;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.hateoas.PagedResources;
import org.springframework.util.StringUtils;
import org.springframework.xd.distributed.util.DefaultDistributedTestSupport;
import org.springframework.xd.distributed.util.DistributedTestSupport;
import org.springframework.xd.rest.client.impl.SpringXDTemplate;
import org.springframework.xd.rest.domain.ModuleMetadataResource;
import org.springframework.xd.rest.domain.StreamDefinitionResource;

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
	 * and all streams destroyed.
	 */
	@After
	public void after() {
		distributedTestSupport.shutdownContainers();
		distributedTestSupport.ensureTemplate().streamOperations().destroyAll();
	}

	/**
	 * After all tests are executed, {@link DistributedTestSupport#shutdownAll}
	 * is invoked if an instance was not provided by the test suite
	 * {@link DistributedTestSuite}.
	 */
	@AfterClass
	public static void afterClass() {
		if (!suiteDetected) {
			distributedTestSupport.shutdownAll();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void startup() {
		distributedTestSupport.startup();
	}

	/**
	 * {@inheritDoc}
	 */
	public SpringXDTemplate ensureTemplate() {
		return distributedTestSupport.ensureTemplate();
	}

	/**
	 * {@inheritDoc}
	 */
	public JavaApplication<SimpleJavaApplication> startContainer(Properties properties) {
		return distributedTestSupport.startContainer(properties);
	}

	/**
	 * {@inheritDoc}
	 */
	public JavaApplication<SimpleJavaApplication> startContainer() {
		return distributedTestSupport.startContainer();
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<Long, String> waitForContainers() throws InterruptedException {
		return distributedTestSupport.waitForContainers();
	}

	/**
	 * {@inheritDoc}
	 */
	public void shutdownContainer(long pid) {
		distributedTestSupport.shutdownContainer(pid);
	}

	/**
	 * {@inheritDoc}
	 */
	public void shutdownContainers() {
		distributedTestSupport.shutdownContainers();
	}

	/**
	 * {@inheritDoc}
	 */
	public void shutdownAll() {
		distributedTestSupport.shutdownAll();
	}

	/**
	 * Block the executing thread until the Admin server reports exactly
	 * two runtime modules (a source and sink).
	 *
	 * @return mapping of modules to the containers they are deployed to
	 * @throws InterruptedException
	 */
	protected ModuleRuntimeContainers retrieveModuleRuntimeContainers()
			throws InterruptedException {

		ModuleRuntimeContainers containers = new ModuleRuntimeContainers();
		long expiry = System.currentTimeMillis() + 30000;
		int moduleCount = 0;

		while (!containers.isComplete() && System.currentTimeMillis() < expiry) {
			Thread.sleep(500);
			moduleCount = 0;
			for (ModuleMetadataResource module : distributedTestSupport.ensureTemplate()
					.runtimeOperations().listRuntimeModules()) {
				String moduleId = module.getModuleId();
				if (moduleId.contains("source")) {
					containers.setSourceContainer(module.getContainerId());
				}
				else if (moduleId.contains("sink")) {
					containers.setSinkContainer(module.getContainerId());
				}
				else {
					throw new IllegalStateException(String.format(
							"Module '%s' is neither a source or sink", moduleId));
				}
				moduleCount++;
			}
		}
		assertTrue(containers.isComplete());
		assertEquals(2, moduleCount);

		return containers;
	}

	/**
	 * Assert that:
	 * <ul>
	 *     <li>The given stream has been created</li>
	 *     <li>It is the only stream in the system</li>
	 * </ul>
	 * @param streamName  name of stream to verify
	 */
	protected void verifySingleStreamCreation(String streamName) {
		PagedResources<StreamDefinitionResource> list = distributedTestSupport.ensureTemplate()
				.streamOperations().list();

		Iterator<StreamDefinitionResource> iterator = list.iterator();
		assertTrue(iterator.hasNext());

		StreamDefinitionResource stream = iterator.next();
		assertEquals(streamName, stream.getName());
		assertFalse(iterator.hasNext());
	}


	/**
	 * Mapping of source and sink modules to the containers they are
	 * deployed to.
	 */
	protected static class ModuleRuntimeContainers {

		private String sourceContainer;

		private String sinkContainer;

		public String getSourceContainer() {
			return sourceContainer;
		}

		public void setSourceContainer(String sourceContainer) {
			this.sourceContainer = sourceContainer;
		}

		public String getSinkContainer() {
			return sinkContainer;
		}

		public void setSinkContainer(String sinkContainer) {
			this.sinkContainer = sinkContainer;
		}

		/**
		 * Return true if a source and sink container have been
		 * populated.
		 *
		 * @return true if source and sink containers are non-null
		 */
		public boolean isComplete() {
			return StringUtils.hasText(sourceContainer) && StringUtils.hasText(sinkContainer);
		}

	}


}
