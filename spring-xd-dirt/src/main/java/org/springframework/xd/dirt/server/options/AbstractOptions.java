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

	public static final String DEFAULT_HOME = "..";

	protected AbstractOptions() {

	}

	protected AbstractOptions(Transport defaultTransport, Analytics defaultAnalytics) {
		this.transport = defaultTransport;
		this.analytics = defaultAnalytics;
	}

	@Option(name = "--help", usage = "Show options help", aliases = { "-?", "-h" })
	protected boolean showHelp = false;

	@Option(name = "--transport", usage = "The transport to be used (default: redis)")
	protected Transport transport = Transport.redis;

	@Option(name = "--xdHomeDir", usage = "The XD installation directory", metaVar = "<xdHomeDir>")
	protected String xdHomeDir = "..";

	@Option(name = "--enableJmx", usage = "Enable JMX in the XD container (default: false)", metaVar = "[true | false]", handler = ExplicitBooleanOptionHandler.class)
	protected boolean jmxEnabled = false;

	@Option(name = "--analytics", usage = "How to persist analytics such as counters and gauges (default: redis)")
	protected Analytics analytics = Analytics.redis;

	@Option(name = "--hadoopDistro", usage = "The Hadoop distro to use (default: hadoop10)")
	protected HadoopDistro hadoopDistro = HadoopDistro.hadoop10;

	public Analytics getAnalytics() {
		return analytics;
	}

	/**
	 * @return the xdHomeDir
	 */
	public String getXDHomeDir() {
		return xdHomeDir;
	}

	public Transport getTransport() {
		return transport;
	}

	public HadoopDistro getHadoopDistro() {
		return hadoopDistro;
	}

	/**
	 * @return jmxEnabled
	 */
	public boolean isJmxEnabled() {
		return jmxEnabled;
	}

	public abstract int getJmxPort();

	/**
	 * @return the showHelp
	 */
	public boolean isShowHelp() {
		return showHelp;
	}
}
