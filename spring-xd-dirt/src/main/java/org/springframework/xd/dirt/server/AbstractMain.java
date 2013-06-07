package org.springframework.xd.dirt.server;

import org.springframework.util.StringUtils;

abstract class AbstractMain {

	static final String XD_HOME_KEY = "xd.home";

	static final String XD_TRANSPORT_KEY = "xd.transport";

	static final String DEFAULT_HOME = "..";

	static final String DEFAULT_TRANSPORT = "redis";

	/**
	 * Set xd.home system property. If not a valid String, fallback to default.
	 */
	static void setXDHome(String home) {
		if (!StringUtils.hasText(home)) {
			home = DEFAULT_HOME;
		}
		System.setProperty(XD_HOME_KEY, home);
	}

	/**
	 * Set xd.transport system property. If not a valid String, fallback to default.
	 */
	static void setXDTransport(String transport) {
		if (!StringUtils.hasText(transport)) {
			transport = DEFAULT_TRANSPORT;
		}
		System.setProperty(XD_TRANSPORT_KEY, transport);
	}

}
