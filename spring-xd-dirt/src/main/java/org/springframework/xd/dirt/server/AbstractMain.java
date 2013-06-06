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
		String xdHome = DEFAULT_HOME;
		if (StringUtils.hasText(home)) {
			xdHome = home;
		}
		// Check if xd.home system property already exists
		else if (System.getProperty(XD_HOME_KEY) != null) {
			xdHome = System.getProperty(XD_HOME_KEY);
		}
		System.setProperty(XD_HOME_KEY, xdHome);
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
