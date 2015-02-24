/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.server.options;

import java.io.IOException;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.Setter;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.util.ConfigLocations;

/**
 * Holds definitions of {@link ResourcePatternScanningOptionHandler}s used in Spring XD.
 *
 * @author Eric Bottard
 * @author Gary Russell
 * @author David Turanski
 */
public final class ResourcePatternScanningOptionHandlers {

	private ResourcePatternScanningOptionHandlers() {
		// prevent instantiation
	}

	private static final String CONFIGURATION_ROOT = "classpath*:/" + ConfigLocations.XD_CONFIG_ROOT;

	/**
	 * Computes values for (data) --transport for the singlenode case.
	 */
	public static class SingleNodeDataTransportOptionHandler extends ResourcePatternScanningOptionHandler {

		public SingleNodeDataTransportOptionHandler(CmdLineParser parser, OptionDef option, Setter<String> setter)
				throws IOException {
			super(parser, option, setter, resolveMessageBusPath());
			possibleValues.add("local");
		}

		private static String resolveMessageBusPath() {
			String xdHome = CommandLinePropertySourceOverridingListener.getCurrentEnvironment().resolvePlaceholders(
					"${XD_HOME}");
			return "file:" + xdHome.replaceAll("\\\\", "/") + "/lib/messagebus/*";
		}

		@Override
		protected boolean shouldConsider(Resource r) {
			try {
				return r.getFile().isDirectory();
			}
			catch (IOException e) {
				return false;
			}
		}
	}

	/**
	 * Computes values for --analytics in the single node case (accepts memory).
	 */
	public static class SingleNodeAnalyticsOptionHandler extends ResourcePatternScanningOptionHandler {

		public SingleNodeAnalyticsOptionHandler(CmdLineParser parser, OptionDef option, Setter<String> setter)
				throws IOException {
			super(parser, option, setter, CONFIGURATION_ROOT + "analytics/*-analytics.xml");
		}
	}

	/**
	 * Computes values for --analytics in the distributed case (memory is NOT supported).
	 */
	public static class DistributedAnalyticsOptionHandler extends ResourcePatternScanningOptionHandler {

		public DistributedAnalyticsOptionHandler(CmdLineParser parser, OptionDef option, Setter<String> setter)
				throws IOException {
			super(parser, option, setter, CONFIGURATION_ROOT + "analytics/*-analytics.xml");
			exclude("memory");
		}
	}

	/**
	 * Computes values for --hadoopDistro out of names of subdirectories of the {@code lib/} dir.
	 */
	public static class HadoopDistroOptionHandler extends ResourcePatternScanningOptionHandler {

		public HadoopDistroOptionHandler(CmdLineParser parser, OptionDef option, Setter<String> setter)
				throws IOException {
			super(parser, option, setter, resolveXDHome());
			exclude("messagebus");
		}

		/**
		 * Attempt to resolve the xd.home placeholder and take care of corner cases where the user may have provided a
		 * trailing slash, etc.
		 */
		private static String resolveXDHome() {
			Assert.state(
					CommandLinePropertySourceOverridingListener.getCurrentEnvironment() != null,
					"Expected to be called in the control flow of "
							+ CommandLinePropertySourceOverridingListener.class.getSimpleName()
							+ ".onApplicationEvent()");
			//TODO: I'm not sure if this works. compare to resolveMessageBusPath() above
			String resolved = CommandLinePropertySourceOverridingListener.getCurrentEnvironment()
					.resolvePlaceholders("${xd.home:.}/lib/*");
			resolved = StringUtils.cleanPath(resolved).replace("//", "/");
			return "file:" + resolved;
		}

		@Override
		protected boolean shouldConsider(Resource r) {
			try {
				return r.getFile().isDirectory();
			}
			catch (IOException e) {
				return false;
			}
		}

	}

}
