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

import org.junit.BeforeClass;

import org.springframework.xd.dirt.module.ModuleDeployer;
import org.springframework.xd.dirt.server.SingleNodeMain;
import org.springframework.xd.dirt.server.SingleNodeServer;
import org.springframework.xd.dirt.server.options.SingleNodeOptions;
import org.springframework.xd.module.Module;

/**
 * @author David Turanski
 */
public abstract class AbstractStreamTests {

	private static StreamDeployer streamDeployer;

	private static ModuleDeployer moduleDeployer;

	@BeforeClass
	public static void startXDSingleNode() throws Exception {
		SingleNodeServer singleNode = SingleNodeMain.launchSingleNodeServer(new SingleNodeOptions());
		streamDeployer = singleNode.getAdminServer().getApplicationContext().getBean(StreamDeployer.class);
		moduleDeployer = singleNode.getContainer().getApplicationContext().getBean(ModuleDeployer.class);
	}


	protected void deployStream(String name, String config) {
		streamDeployer.save(new StreamDefinition(name, config));
		streamDeployer.deploy(name);
	}

	protected void undeployStream(String name) {
		streamDeployer.undeploy(name);
	}

	protected Module getDeployedModule(String streamName, int index) {
		return moduleDeployer.getDeployedModules().get(streamName).get(index);
	}
}
