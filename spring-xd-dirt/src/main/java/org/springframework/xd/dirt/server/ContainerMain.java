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

import org.springframework.context.ApplicationContext;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.dirt.launcher.ContainerLauncher;
import org.springframework.xd.dirt.launcher.ContainerLauncherFactory;
import org.springframework.xd.dirt.server.options.CommandLineParser;
import org.springframework.xd.dirt.server.options.ContainerOptions;

/**
 * The main driver class for the container
 * 
 * @author Mark Pollack
 * @author Jennifer Hickey
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 * @author David Turanski
 */
public class ContainerMain {

	private static final Log logger = LogFactory.getLog(ContainerMain.class);

	/**
	 * The main entry point to create the XDContainer.
	 * 
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		launchContainer(parseCommandLineOptions(args), null);
	}

	/**
	 * Parse command line options from a String array into a type safe ContainerOptions class. If any options are not
	 * valid, an InvalidCommandLineArgumentException exception is thrown
	 * 
	 * @param args command line arguments
	 * @return type safe ContainerOptions if all command line arguments are valid
	 * @throws InvalidCommandLineArgumentException if there is an invalid command line argument
	 */
	public static ContainerOptions parseOptions(String[] args) {
		ContainerOptions options = new ContainerOptions();
		CommandLineParser parser = new CommandLineParser(options);

		parser.parseArgument(args);

		return options;
	}

	/**
	 * Parse command line options from a String array into a type safe ContainerOptions class. If any options are not
	 * valid, a help message is displayed and System.exit is called. If the help option is passed, display the usage.
	 * 
	 * @param args command line arguments
	 * @return type safe ContainerOptions if all command line arguments are valid
	 */
	private static ContainerOptions parseCommandLineOptions(String[] args) {
		ContainerOptions options = new ContainerOptions();
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
		return options;
	}

	/**
	 * Create a new instance of XDContainer given ContainerOptions and an optional parent ApplicationContext
	 * 
	 * @param options The options that select transport, analytics, and other infrastructure options.
	 * @param parentContext an optional parent context to set on the XDContainer's ApplicationContext.
	 * @return a new XDContainer instance
	 */
	public static XDContainer launchContainer(ContainerOptions options, ApplicationContext parentContext) {
		ContainerLauncherFactory containerLauncherFactory = new ContainerLauncherFactory();
		ContainerLauncher launcher = containerLauncherFactory.createContainerLauncher(options, parentContext);
		XDContainer container = launcher.launch(options);
		return container;
	}
}
