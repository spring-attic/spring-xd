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
 *
 * @author Mark Pollack
 */
public class ContainerOptions {

	@Option(name="--xdHomeDir", usage="The XD installation directory", metaVar="<xdHomeDir>")
	private final String xdHomeDir = "";

	@Option(name="--transport", usage="The transport to be used (redis, rabbit, local)")
	private final String transport = "";

	@Option(name="--help", usage="Show options help", aliases={"-?", "-h"})
	private final boolean showHelp = false;

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

}
