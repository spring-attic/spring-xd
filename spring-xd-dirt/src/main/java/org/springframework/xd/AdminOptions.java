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
package org.springframework.xd;

import org.kohsuke.args4j.Option;

/**
 * A class the defines the options that will be parsed on the command line
 * @author Mark Pollack
 *
 */
public class AdminOptions {
	
	//@Option(name="--redisHost", usage="the hostname of the redis sever", metaVar="<redisHost>")
	private String redisHost = "localhost";
	
	//@Option(name="--redisPort", usage="the port number of the redis sever", metaVar="<redisPort>")
	private int redisPort = 6379;
	
	@Option(name="--help", usage="Show options help", aliases={"-?", "-h"})
	private boolean showHelp = false;
	
	@Option(name="--xdHomeDir", usage="The XD installation directory, use with --embeddedAdmin", metaVar="<xdHomeDir>")
	private String xdHomeDir = "";
	
	@Option(name="--embeddedContainer", usage="embed the XD Container")
	private boolean embeddedContainer = false;
	/**
	 * @return the showHelp
	 */
	public boolean isShowHelp() {
		return showHelp;
	}
	/**
	 * @return the redisHost
	 */
	public String getRedisHost() {
		return redisHost;
	}
	/**
	 * @return the redisPort
	 */
	public int getRedisPort() {
		return redisPort;
	}

	/**
	 * @return the moduleDir
	 */
	public String getXDHomeDir() {
		return xdHomeDir;
	}
	/**
	 * @return the xdHomeDir
	 */
	public String getXdHomeDir() {
		return xdHomeDir;
	}
	/**
	 * @return the embeddedContainer
	 */
	public boolean isEmbeddedContainer() {
		return embeddedContainer;
	}
	
	
	
	
}
