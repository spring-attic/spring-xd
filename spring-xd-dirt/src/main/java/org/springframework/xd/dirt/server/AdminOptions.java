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

import org.kohsuke.args4j.Option;

/**
 * A class the defines the options that will be parsed on the command line
 * @author Mark Pollack
 *
 */
public class AdminOptions {

	@Option(name="--help", usage="Show options help", aliases={"-?", "-h"})
	private boolean showHelp = false;

	@Option(name="--xdHomeDir", usage="The XD installation directory", metaVar="<xdHomeDir>")
	private String xdHomeDir = "";

	@Option(name="--transport", usage="The transport to be used (redis, rabbit, local)", metaVar="<transport>")
	private String transport = "";
	
	@Option(name="--httpPort", usage="Http port for the stream server(default: 8080)", metaVar="<httpPort>")
	private String httpPort = "8080";

	/**
	 * @return the showHelp
	 */
	public boolean isShowHelp() {
		return showHelp;
	}

	/**
	 * @return the xdHomeDir
	 */
	public String getXDHomeDir() {
		return xdHomeDir;
	}

	/**
	 * @return the transport
	 */
	public String getTransport() {
		return transport;
	}
	
	/**
	 * @return http port
	 */
	public String getHttpPort() {
		return httpPort;
	}

}
