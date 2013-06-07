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
import org.springframework.xd.dirt.stream.StreamServer;


/**
 * The main driver class for the admin
 *
 * @author Mark Pollack
 * @author Jennifer Hickey
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
public class AdminMain extends AbstractMain {

	private static final Log logger = LogFactory.getLog(AdminMain.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		AdminOptions options = new  AdminOptions();
		CmdLineParser parser = new CmdLineParser(options);
		try {
			parser.parseArgument(args);
		}
		catch (CmdLineException e) {
			logger.error(e.getMessage());
			parser.printUsage(System.err);
			System.exit(1);
		}

		setXDHome(options.getXDHomeDir());
		setXDTransport(options.getTransport());
		setHttpPort(options.getHttpPort());

		if (options.isShowHelp()) {
			parser.printUsage(System.err);
			System.exit(0);
		}

		launchStreamServer(System.getProperty(XD_HOME_KEY));
	}
	
	/**
	 * Set http port for the admin server from command line option
	 * @param httpPort
	 */
	private static void setHttpPort(String httpPort) {
		if(StringUtils.hasText(httpPort)) {
			System.setProperty(XD_ADMIN_HTTP_PORT_KEY, httpPort);
		}
	}

	/**
	 * Launch stream server with the given home and transport
	 */
	public static void launchStreamServer(String home) {
		StreamServer.main(new String[]{home});
	}

}
