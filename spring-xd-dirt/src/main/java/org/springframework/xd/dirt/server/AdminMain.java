/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.xd.dirt.server.options.AdminOptions;
import org.springframework.xd.dirt.server.options.CommandLineParser;
import org.springframework.xd.dirt.server.options.Transport;


/**
 * The main driver class for the Admin Server.
 * 
 * @author Mark Pollack
 * @author Jennifer Hickey
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 * @author Eric Bottard
 * @author David Turanski
 */
public class AdminMain {

	private static final Log logger = LogFactory.getLog(AdminMain.class);

	/**
	 * The main entry point to create the AdminServer.
	 * 
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		launchAdminServer(parseCommandLineOptions(args));
	}

	/**
	 * Parse command line options from a String array into a type safe AdminOptions class. If any options are not valid,
	 * an InvalidCommandLineArgumentException exception is thrown
	 * 
	 * @param args command line arguments
	 * @return type safe AdminOptions if all command line arguments are valid
	 * @throws InvalidCommandLineArgumentException if there is an invalid command line argument
	 */
	public static AdminOptions parseOptions(String[] args) {
		AdminOptions options = new AdminOptions();
		CommandLineParser parser = new CommandLineParser(options);

		parser.parseArgument(args);

		return options;
	}

	/**
	 * Parse command line options from a String array into a type safe AdminOptions class. if any options are not valid,
	 * a help message is displayed and System.exit is called. If the help option is passed, display the usage.
	 * 
	 * @param args command line arguments
	 * @return type safe AdminOptions if all command line arguments are valid
	 */
	private static AdminOptions parseCommandLineOptions(String[] args) {
		AdminOptions options = new AdminOptions();
		CommandLineParser parser = new CommandLineParser(options);
		try {
			parser.parseArgument(args);
		}
		catch (InvalidCommandLineArgumentException e) {
			logger.error(e.getMessage());
			parser.printUsage(System.err);
			System.exit(1);
		}
		if (options.isShowHelp()) {
			parser.printUsage(System.err);
			System.exit(0);
		}

		if (options.getTransport() == Transport.local) {
			logger.error("local transport is not supported. Run SingleNodeMain");
			System.exit(1);
		}

		return options;
	}

	/**
	 * Create a new instance of the AdminServer given AdminOptions
	 * 
	 * @param options The options that select transport, analytics, and other infrastructure options.
	 * @return a new AdminServer instance
	 */
	public static AdminServer launchAdminServer(final AdminOptions options) {
		final AdminServer server = new AdminServer(options);
		server.run();
		return server;
	}
}
