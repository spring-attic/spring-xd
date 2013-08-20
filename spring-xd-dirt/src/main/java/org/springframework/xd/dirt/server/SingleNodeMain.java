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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import org.springframework.context.ApplicationContext;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.xd.dirt.container.DefaultContainer;
import org.springframework.xd.dirt.core.Container;
import org.springframework.xd.dirt.server.options.AdminOptions;
import org.springframework.xd.dirt.server.options.ContainerOptions;
import org.springframework.xd.dirt.server.options.SingleNodeOptions;
import org.springframework.xd.dirt.server.options.Transport;
import org.springframework.xd.dirt.stream.StreamServer;


/**
 * 
 * @author David Turanski
 */
public class SingleNodeMain {

	private static final Log logger = LogFactory.getLog(SingleNodeMain.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SingleNodeOptions options = parseOptions(args);
		StreamServer server = launchStreamServer(options);
		Container container =launchContainer(options.asContainerOptions());
		setUpControlChannels(server, container);
	}

	public static StreamServer launchStreamServer(AdminOptions options) {
		return AdminMain.launchStreamServer(options);
	}

	public static Container launchContainer(ContainerOptions options) {
		return ContainerMain.launch(options);
	}

	public static SingleNodeOptions parseOptions(String[] args) {
		SingleNodeOptions options = new SingleNodeOptions();
		CmdLineParser parser = new CmdLineParser(options);
		try {
			parser.parseArgument(args);
		}
		catch (CmdLineException e) {
			logger.error(e.getMessage());
			parser.printUsage(System.err);
			System.exit(1);
		}
		if (options.isShowHelp()) {
			parser.printUsage(System.err);
			System.exit(0);
		}
		if (!options.getTransport().equals(Transport.local)) {
			logger.error("Only local transport is currently supported for XD Single Node");
			System.exit(1);
		}

		return options;
	}

	public static void setUpControlChannels(StreamServer streamServer, Container container) {
		DefaultContainer defaultContainer = (DefaultContainer) container;
		ApplicationContext containerContext = defaultContainer.getApplicationContext();

		MessageChannel containerControlChannel = containerContext.getBean("input", MessageChannel.class);

		ApplicationContext adminContext = streamServer.getXmlWebApplicationContext();

		SubscribableChannel deployChannel = adminContext.getBean("deployChannel", SubscribableChannel.class);
		SubscribableChannel undeployChannel = adminContext.getBean("undeployChannel", SubscribableChannel.class);

		BridgeHandler handler = new BridgeHandler();
		handler.setOutputChannel(containerControlChannel);
		handler.setComponentName("xd.local.control.bridge");
		deployChannel.subscribe(handler);
		undeployChannel.subscribe(handler);
	}

}
