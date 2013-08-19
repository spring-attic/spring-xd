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

package org.springframework.xd.dirt.server.options;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;
import org.springframework.util.StringUtils;

/**
 * Options shared by both the admin and the container server.
 * 
 * @author Eric Bottard
 * @author Mark Pollack
 * @author David Turanski
 * @author Mark Fisher
 */
public abstract class AbstractOptions {

	public static final String DEFAULT_HOME = "..";

	private static final String XD_PAYLOAD_TRANSFORMER_KEY = "xd.payload.transformer";

	public static final String XD_HOME_KEY = "xd.home";

	public static final String XD_TRANSPORT_KEY = "xd.transport";

	public static final String XD_ENABLED_JMX_KEY = "xd.jmx.enabled";

	private static final String XD_ANALYTICS_KEY = "xd.analytics";

	// TODO: Technically it's a REST port for MBEAN resources rather than a JSR-160 port.
	// Is the name misleading?
	public static final String XD_JMX_PORT_KEY = "xd.jmx.port";

	/**
	 * Set xd.home system property. If not a valid String, fallback to default.
	 */
	public static void setXDHome(String home) {
		boolean override = StringUtils.hasText(home);
		if (override || System.getProperty(XD_HOME_KEY) == null) {
			System.setProperty(XD_HOME_KEY, (override ? home : DEFAULT_HOME));
		}
	}

	/**
	 * Set xd.transport system property.
	 */
	public static void setXDTransport(Transport transport) {
		System.setProperty(XD_TRANSPORT_KEY, transport.name());
	}

	/**
	 * Set xd.analytics system property.
	 */
	public static void setXDAnalytics(Analytics analytics) {
		System.setProperty(XD_ANALYTICS_KEY, analytics.name());
	}

	@Option(name = "--help", usage = "Show options help", aliases = { "-?", "-h" })
	private boolean showHelp = false;

	@Option(name = "--transport", usage = "The transport to be used (default: redis)")
	private Transport transport = Transport.redis;

	@Option(name = "--xdHomeDir", usage = "The XD installation directory", metaVar = "<xdHomeDir>")
	private String xdHomeDir = ""; // Can't set default here as it may have been set via
									// -Dxd.home=foo

	@Option(name = "--enableJmx", usage = "Enable JMX in the XD container (default: false", handler = JmxEnabledHandler.class)
	private boolean jmxEnabled = false;

	@Option(name = "--transformer", usage = "The default payload transformer class name", handler = PayloadTransformerHandler.class)
	private String transformer;

	@Option(name = "--analytics", usage = "How to persist analytics such as counters and gauges (default: redis)")
	private Analytics analytics = Analytics.redis;

	@Option(name = "--hadoopDistro", usage = "The Hadoop distro to use (default: hadoop10)")
	private HadoopDistro hadoopDistro = HadoopDistro.hadoop10;

	public Analytics getAnalytics() {
		return analytics;
	}

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
	 * @return jmxEnabled
	 */
	public boolean isJmxEnabled() {
		return Boolean.getBoolean(XD_ENABLED_JMX_KEY);
	}

	public abstract int getJmxPort();

	public String getTransformer() {
		return transformer;
	}

	/**
	 * @return the showHelp
	 */
	public boolean isShowHelp() {
		return showHelp;
	}

	public static class PayloadTransformerHandler extends OptionHandler<String> {

		public PayloadTransformerHandler(CmdLineParser parser, OptionDef option, Setter<String> setter) {
			super(parser, option, setter);
		}

		@Override
		public int parseArguments(Parameters params) throws CmdLineException {
			System.setProperty(XD_PAYLOAD_TRANSFORMER_KEY, params.getParameter(0));
			return 1;
		}

		@Override
		public String getDefaultMetaVariable() {
			return "<transformer>";
		}

	}

	public static class JmxEnabledHandler extends OptionHandler<Boolean> {

		/**
		 * @param parser
		 * @param option
		 * @param setter
		 */
		public JmxEnabledHandler(CmdLineParser parser, OptionDef option, Setter<Boolean> setter) {
			super(parser, option, setter);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.kohsuke.args4j.spi.OptionHandler#parseArguments(org.kohsuke.args4j.spi.
		 * Parameters)
		 */
		@Override
		public int parseArguments(Parameters params) throws CmdLineException {
			System.setProperty(XD_ENABLED_JMX_KEY, "true");
			return 1;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.kohsuke.args4j.spi.OptionHandler#getDefaultMetaVariable()
		 */
		@Override
		public String getDefaultMetaVariable() {
			return null;
		}
	}

}