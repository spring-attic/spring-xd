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

import org.springframework.util.StringUtils;

/**
 * Options shared by both the admin and the container server.
 *
 * @author Eric Bottard
 * @author Mark Pollack
 * @author David Turanski
 * @author Mark Fisher
 */
public class AbstractOptions {

	static final String DEFAULT_HOME = "..";

	static final String XD_HOME_KEY = "xd.home";

	static final String XD_TRANSPORT_KEY = "xd.transport";

	/**
	 * Set xd.home system property. If not a valid String, fallback to default.
	 */
	static void setXDHome(String home) {
		boolean override = StringUtils.hasText(home);
		if (override || System.getProperty(XD_HOME_KEY) == null) {
			System.setProperty(XD_HOME_KEY, (override ? home : DEFAULT_HOME));
		}
	}

	/**
	 * Set xd.transport system property.
	 */
	static void setXDTransport(Transport transport) {
		System.setProperty(XD_TRANSPORT_KEY, transport.name());
	}

	@Option(name = "--help", usage = "Show options help", aliases = { "-?",	"-h" })
	private boolean showHelp = false;

	@Option(name = "--transport", usage = "The transport to be used (default: redis)")
	private Transport transport = Transport.redis;

	@Option(name = "--xdHomeDir", usage = "The XD installation directory", metaVar = "<xdHomeDir>")
	private String xdHomeDir = ""; // Can't set default here as it may have been set via -Dxd.home=foo

	/**
	 * @return the transport
	 */
	public Transport getTransport() {
		return transport;
	}

	/**
	 * @return the xdHomeDir
	 */
	public String getXDHomeDir() {
		return xdHomeDir;
	}

	/**
	 * @return the showHelp
	 */
	public boolean isShowHelp() {
		return showHelp;
	}

}
