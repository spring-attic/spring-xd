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
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.dirt.server.options.AdminOptions;
import org.springframework.xd.dirt.server.options.ContainerOptions;
import org.springframework.xd.dirt.server.options.SingleNodeOptions;
import org.springframework.xd.dirt.server.options.Transport;


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
		launch(parseOptions(args));
	}

	public static SingleNodeServer launch(SingleNodeOptions options) {
		AdminServer adminServer = launchAdminServer(options);

		XDContainer container = launchContainer(options.asContainerOptions(),
				adminServer.getApplicationContext().getParent());
		return new SingleNodeServer(adminServer, container);
	}

	private static AdminServer launchAdminServer(AdminOptions options) {
		final AdminServer server = new AdminServer(options);
		server.run();
		return server;
	}

	private static XDContainer launchContainer(ContainerOptions options, ApplicationContext parentContext) {
		return ContainerMain.launch(options, parentContext);
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

}
