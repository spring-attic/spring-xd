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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.ExplicitBooleanOptionHandler;


/**
 * Options shared by both the admin and the container server.
 * 
 * @author Eric Bottard
 * @author Mark Pollack
 * @author David Turanski
 * @author Mark Fisher
 */
public abstract class AbstractOptions {

	private final List<String> explicitArgs = new ArrayList<String>();

	public static final String DEFAULT_HOME = "..";

	private static final String JMX_ENABLED = "enableJmx";

	private static final String HADOOP_DISTRO = "hadoopDistro";

	private static final String TRANSPORT = "transport";

	private static final String XD_HOME_DIR = "xdHomeDir";

	private static final String ANALYTICS = "analytics";

	protected static final String JMX_PORT = "jmxPort";

	protected final Map<Object, Boolean> optionMetadataCache = new HashMap<Object, Boolean>();

	ParserEventListener getParserEventListener() {
		return new ParserEventListener() {

			@Override
			public void handleEvent(ParserEvent event) {
				switch (event.getEvent()) {
					case parser_started:
						optionMetadataCache.clear();
						explicitArgs.clear();
						break;
					case option_explicit:
						String optionName = ((ParserArgEvent) event).getOptionName();
						explicitArgs.add(optionName);
						break;
					case parser_complete:
						createOptionMetadataCache();
						break;
					default:
				}
			}
		};
	}

	protected AbstractOptions(Transport defaultTransport, Analytics defaultAnalytics) {
		this.transport = defaultTransport;
		this.analytics = defaultAnalytics;
	}

	@Option(name = "--help", usage = "Show options help", aliases = { "-?", "-h" })
	protected boolean showHelp = false;

	@Option(name = "--" + TRANSPORT, usage = "The transport to be used (default: redis)")
	protected Transport transport = Transport.redis;

	@Option(name = "--" + XD_HOME_DIR, usage = "The XD installation directory", metaVar = "<xdHomeDir>")
	protected String xdHomeDir = "..";

	@Option(name = "--enableJmx", usage = "Enable JMX in the XD container (default: false)", metaVar = "[true | false]", handler = ExplicitBooleanOptionHandler.class)
	protected boolean jmxEnabled = false;

	@Option(name = "--" + ANALYTICS, usage = "How to persist analytics such as counters and gauges (default: redis)")
	protected Analytics analytics = Analytics.redis;

	@Option(name = "--" + HADOOP_DISTRO, usage = "The Hadoop distro to use (default: hadoop12)")
	protected HadoopDistro hadoopDistro = HadoopDistro.hadoop12;


	/**
	 * @return analytics
	 */
	public Analytics getAnalytics() {
		return analytics;
	}

	/**
	 * @return xdHomeDir
	 */
	public String getXDHomeDir() {
		return xdHomeDir;
	}

	/**
	 * @return transport
	 */
	public Transport getTransport() {
		return transport;
	}

	/**
	 * @return hadoopDistro
	 */
	public HadoopDistro getHadoopDistro() {
		return hadoopDistro;
	}

	/**
	 * @return jmxEnabled
	 */
	public Boolean isJmxEnabled() {
		return jmxEnabled;
	}

	protected void createOptionMetadataCache()
	{
		optionMetadataCache.put(getAnalytics(), isArg(ANALYTICS));
		optionMetadataCache.put(getTransport(), isArg(TRANSPORT));
		optionMetadataCache.put(getXDHomeDir(), isArg(XD_HOME_DIR));
		optionMetadataCache.put(isJmxEnabled(), isArg(JMX_ENABLED));
		optionMetadataCache.put(getHadoopDistro(), isArg(HADOOP_DISTRO));
		optionMetadataCache.put(getJmxPort(), isArg(JMX_PORT));
	}

	protected boolean isArg(String optionName) {
		return explicitArgs.contains(optionName);
	}

	public abstract Integer getJmxPort();

	public boolean isExplicit(Object option) {
		Boolean explicit = optionMetadataCache.get(option);
		return explicit == null ? false : explicit.booleanValue();
	}

	/**
	 * @return the showHelp
	 */
	public boolean isShowHelp() {
		return showHelp;
	}
}
