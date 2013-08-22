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

import org.springframework.context.ApplicationContext;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.handler.BridgeHandler;


/**
 * @author David Turanski
 */
public class SingleNodeStreamDeploymentIntegrationTests extends AbstractStreamDeploymentIntegrationTests {

	@Override
	protected String getTransport() {
		return "local";
	}

	@Override
	protected void setupApplicationContext(ApplicationContext context) {
		MessageChannel containerControlChannel = context.getBean("containerControlChannel", MessageChannel.class);
		SubscribableChannel deployChannel = context.getBean("deployChannel", SubscribableChannel.class);
		SubscribableChannel undeployChannel = context.getBean("undeployChannel", SubscribableChannel.class);

		BridgeHandler handler = new BridgeHandler();
		handler.setOutputChannel(containerControlChannel);
		handler.setComponentName("xd.local.control.bridge");
		deployChannel.subscribe(handler);
		undeployChannel.subscribe(handler);
	}

	@Override
	protected void cleanup(ApplicationContext context) {

	}

}
