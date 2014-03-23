/*
 * Copyright 2013-2014 the original author or authors.
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

import org.springframework.xd.dirt.util.ConfigLocations;

/**
 * Holds definitions of {@link ResourcePatternScanningOptionHandler}s used in Spring XD.
 * 
 * @author Eric Bottard
 */
public final class ResourcePatternScanningOptionHandlers {

	private ResourcePatternScanningOptionHandlers() {
		// prevent instantiation
	}

	private static final String CONFIGURATION_ROOT = "classpath*:/" + ConfigLocations.XD_CONFIG_ROOT;

	/**
	 * Computes values for (data) --transport for the distributed case.
	 */
	public static class DistributedDataTransportOptionHandler extends ResourcePatternScanningOptionHandler {

		public DistributedDataTransportOptionHandler(CmdLineParser parser, OptionDef option, Setter<String> setter)
				throws IOException {
			super(parser, option, setter, CONFIGURATION_ROOT + "transports/*-bus.xml");
			exclude("local");
		}
	}

	/**
	 * Computes values for (data) --transport for the singlenode case.
	 */
	public static class SingleNodeDataTransportOptionHandler extends ResourcePatternScanningOptionHandler {

		public SingleNodeDataTransportOptionHandler(CmdLineParser parser, OptionDef option, Setter<String> setter)
				throws IOException {
			super(parser, option, setter, CONFIGURATION_ROOT + "transports/*-bus.xml");
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
	 * Computes values for --store in the distributed case (memory is NOT supported).
	 */
	public static class DistributedStoreOptionHandler extends ResourcePatternScanningOptionHandler {

		public DistributedStoreOptionHandler(CmdLineParser parser, OptionDef option, Setter<String> setter)
				throws IOException {
			super(parser, option, setter, CONFIGURATION_ROOT + "store/*-store.xml");
			exclude("memory");
		}
	}

	/**
	 * Computes values for --store in the singlenode case.
	 */
	public static class SingleNodeStoreOptionHandler extends ResourcePatternScanningOptionHandler {

		public SingleNodeStoreOptionHandler(CmdLineParser parser, OptionDef option, Setter<String> setter)
				throws IOException {
			super(parser, option, setter, CONFIGURATION_ROOT + "store/*-store.xml");
		}
	}

}
