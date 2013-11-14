/*
 * Copyright 2013 the original author or authors.
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

import org.springframework.context.ApplicationContext;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.xd.dirt.container.XDContainer;


/**
 * An XD server configured for a single node
 * 
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 */
public class SingleNodeServer {

	private final XDContainer container;

	private final AdminServer adminServer;

	public SingleNodeServer(AdminServer adminServer, XDContainer container) {
		this.container = container;
		this.adminServer = adminServer;
		setUpControlChannels(this.adminServer, this.container);
	}

	public XDContainer getContainer() {
		return container;
	}


	public AdminServer getAdminServer() {
		return adminServer;
	}

	private void setUpControlChannels(AdminServer adminServer, XDContainer container) {
		ApplicationContext containerContext = container.getApplicationContext();

		MessageChannel containerControlChannel = containerContext.getBean("containerControlChannel",
				MessageChannel.class);

		ApplicationContext adminContext = adminServer.getApplicationContext();

		SubscribableChannel deployChannel = adminContext.getBean("deployChannel", SubscribableChannel.class);
		SubscribableChannel undeployChannel = adminContext.getBean("undeployChannel", SubscribableChannel.class);

		BridgeHandler handler = new BridgeHandler();
		handler.setOutputChannel(containerControlChannel);
		handler.setComponentName("xd.local.control.bridge");
		deployChannel.subscribe(handler);
		undeployChannel.subscribe(handler);
	}

	public void stop() {
		this.getContainer().stop();
		this.getAdminServer().stop();
	}
}
