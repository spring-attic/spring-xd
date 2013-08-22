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

import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.dirt.server.options.SingleNodeOptions;
import org.springframework.xd.dirt.server.options.Transport;


/**
 * The main driver class for the SingleNode Server that has both the AdminServer and XDContainer
 * 
 * @author David Turanski
 */
public class SingleNodeMain {

	private static final Log logger = LogFactory.getLog(SingleNodeMain.class);

	/**
	 * The main entry point to create the AdminServer.
	 * 
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		launchSingleNodeServer(parseCommandLineOptions(args));
	}

	/**
	 * Parse command line options from a String array into a type safe SingleNodeOptions class. If any options are not
	 * valid, an InvalidCommandLineArgumentException exception is thrown
	 * 
	 * @param args command line arguments
	 * @return type safe SingleNodeOptions if all command line arguments are valid
	 * @throws InvalidCommandLineArgumentException if there is an invalid command line argument
	 */
	public static SingleNodeOptions parseOptions(String[] args) {
		SingleNodeOptions options = new SingleNodeOptions();
		CmdLineParser parser = new CmdLineParser(options);
		try {
			parser.parseArgument(args);

		}
		catch (CmdLineException e) {
			logger.error(e.getMessage());
			throw new InvalidCommandLineArgumentException(e.getMessage(), e);
		}
		return options;
	}

	/**
	 * Parse command line options from a String array into a type safe SingleNodeOptions class. If any options are not
	 * valid, a help message is displayed and System.exit is called. If the help option is passed, display the usage.
	 * 
	 * @param args command line arguments
	 * @return type safe SingleNodeOptions if all command line arguments are valid
	 */
	private static SingleNodeOptions parseCommandLineOptions(String[] args) {
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

	/**
	 * Create a new instance of SingleNodeServer given SingleNodeOptions
	 * 
	 * @param options The options that select transport, analytics, and other infrastructure options.
	 * @return a new SingleNodeServer instance
	 */
	public static SingleNodeServer launchSingleNodeServer(SingleNodeOptions options) {
		AdminServer adminServer = AdminMain.launchAdminServer(options);
		XDContainer container = ContainerMain.launchContainer(options.asContainerOptions(),
				adminServer.getApplicationContext().getParent());
		return new SingleNodeServer(adminServer, container);
	}

}
