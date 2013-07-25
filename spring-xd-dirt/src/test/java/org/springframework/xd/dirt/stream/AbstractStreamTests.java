/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.dirt.stream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.xd.dirt.server.options.AbstractOptions;
import org.springframework.xd.dirt.server.options.AdminOptions;
import org.springframework.xd.dirt.server.options.Store;
import org.springframework.xd.dirt.server.options.Transport;

/**
 * @author David Turanski
 */
public abstract class AbstractStreamTests {

	static StreamDeployer streamDeployer;

	@BeforeClass
	public static void startXDSingleNode() throws Exception {
		System.setProperty("xd.home", "..");
		AbstractOptions.setXDTransport(Transport.local);
		AdminOptions.setXDStore(Store.memory);

		ApplicationContext ctx = new ClassPathXmlApplicationContext("/META-INF/spring-xd/transports/local-admin.xml",
				"/META-INF/spring-xd/store/memory-admin.xml");
		streamDeployer = ctx.getBean(StreamDeployer.class);

	}

	@AfterClass
	public static void resetSystemProperties() {
		System.clearProperty("xd.transport");
		System.clearProperty("xd.home");
		System.clearProperty("xd.store");
	}

	protected void deployStream(String name, String config) {
		streamDeployer.save(new StreamDefinition(name, config));
		streamDeployer.deploy(name);
	}

	protected void undeployStream(String name) {
		streamDeployer.undeploy(name);
	}

}
