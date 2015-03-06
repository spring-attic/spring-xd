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

package org.springframework.xd.dirt.server;

import org.junit.Assert;
import org.junit.Test;

import org.springframework.mock.env.MockEnvironment;
import org.springframework.xd.dirt.cluster.ContainerAttributes;
import org.springframework.xd.dirt.server.container.ContainerServerApplication;
import org.springframework.xd.dirt.util.RuntimeUtils;


/**
 * Helper class that bootstraps random configuration for dirt server applications.
 *
 * @author Gunnar Hillert
 */
public class ContainerServerApplicationTests {

	private static final String CONTAINER_IP_KEY = ContainerServerApplication.CONTAINER_ATTRIBUTES_PREFIX
			+ ContainerAttributes.IP_ADDRESS_KEY;

	private static final String CONTAINER_HOST_KEY = ContainerServerApplication.CONTAINER_ATTRIBUTES_PREFIX
			+ ContainerAttributes.HOST_KEY;

	@Test
	public void testDefaultContainerAttributes() {

		final String defaultIp = RuntimeUtils.getIpAddress();
		final String defaultHostname = RuntimeUtils.getHost();

		MockEnvironment environment = new MockEnvironment();

		final ContainerServerApplication containerServerApplication = new ContainerServerApplication();
		containerServerApplication.setEnvironment(environment);

		final ContainerAttributes containerAttributes = containerServerApplication.containerAttributes();
		Assert.assertNotNull(containerAttributes);
		Assert.assertEquals(defaultIp, containerAttributes.getIp());
		Assert.assertEquals(defaultHostname, containerAttributes.getHost());

	}

	@Test
	public void testContainerAttributesWithCustomContainerIpAndHostname() {

		final String customIp = "123.123.123.123";
		final String customHostname = "testhost";

		MockEnvironment environment = new MockEnvironment();
		environment.setProperty(CONTAINER_IP_KEY, customIp);
		environment.setProperty(CONTAINER_HOST_KEY, customHostname);

		final ContainerServerApplication containerServerApplication = new ContainerServerApplication();
		containerServerApplication.setEnvironment(environment);

		final ContainerAttributes containerAttributes = containerServerApplication.containerAttributes();
		Assert.assertNotNull(containerAttributes);
		Assert.assertEquals(customIp, containerAttributes.getIp());
		Assert.assertEquals(customHostname, containerAttributes.getHost());
	}

	@Test
	public void testContainerAttributesWithCustomEmptyStringContainerIpAndHostname() {

		final String defaultIp = RuntimeUtils.getIpAddress();
		final String defaultHostname = RuntimeUtils.getHost();

		final String customIp = "";
		final String customHostname = "";

		MockEnvironment environment = new MockEnvironment();
		environment.setProperty(CONTAINER_IP_KEY, customIp);
		environment.setProperty(CONTAINER_HOST_KEY, customHostname);

		final ContainerServerApplication containerServerApplication = new ContainerServerApplication();
		containerServerApplication.setEnvironment(environment);

		final ContainerAttributes containerAttributes = containerServerApplication.containerAttributes();
		Assert.assertNotNull(containerAttributes);
		Assert.assertEquals(defaultIp, containerAttributes.getIp());
		Assert.assertEquals(defaultHostname, containerAttributes.getHost());
	}
}
