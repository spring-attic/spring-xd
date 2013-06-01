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
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.launcher.RedisContainerLauncher;

/**
 * The main driver class for ContainerMain 
 * @author Mark Pollack
 * @author Jennifer Hickey
 * @author Ilayaperumal Gopinathan
 */
public class ContainerMain {

	private static final Log logger = LogFactory.getLog(ContainerMain.class);

	/**
	 * Start the RedisContainerLauncher
	 * @param args command line argument
	 */
	public static void main(String[] args) {
		ContainerOptions options = new  ContainerOptions();
		CmdLineParser parser = new CmdLineParser(options);
		String registryType = "redis";
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			logger.error(e.getMessage());
			parser.printUsage(System.err);
			System.exit(1);
		}

		// Override registry.type system property if commandLine option is set
		if (!options.getRegistryType().equals("redis")) {
			registryType = options.getRegistryType();
		}
		// Override xd.home system property if commandLine option is set
		if (StringUtils.hasText(options.getXDHomeDir())) {
			System.setProperty("xd.home", options.getXDHomeDir());
		}	
		String xdHome = setXDHome();

		if (options.isShowHelp()) {
			parser.printUsage(System.err);
			System.exit(0);
		}

		if (options.isEmbeddedAdmin() == true ) {
			AdminMain.launchStreamServer(xdHome, registryType);
		}

		//Future versions to support other types of container launchers
		RedisContainerLauncher.main(new String[]{xdHome, registryType});
	}

	/**
	 * Set xd.home system property to relative path if it is not set already.
	 * This could happen when the AdminMain is not launched from xd scripts.
	 * @return String
	 */
	private static String setXDHome() {
		String xdhome = System.getProperty("xd.home");
		// Make sure to set xd.home system property
		if (!StringUtils.hasText(xdhome)) {
			// if xd.home system property is not set,
			// then set it to relative path
			xdhome = "..";
			// Set system property for embedded container if exists
			System.setProperty("xd.home", xdhome);
		}
		return xdhome;
	}
}
