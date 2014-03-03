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


/**
 * Holds definitions of {@link FromResourceLocationOptionHandler}s used in Spring XD.
 * 
 * @author Eric Bottard
 */
public final class FromResourceLocationOptionHandlers {

	private FromResourceLocationOptionHandlers() {

	}

	public static final String SINGLE_NODE_SPECIAL_CONTROL_TRANSPORT = "local";

	/**
	 * Computes values for --controlTransport for the distributed case.
	 */
	public static class DistributedControlTransportOptionHandler extends FromResourceLocationOptionHandler {

		public DistributedControlTransportOptionHandler(CmdLineParser parser, OptionDef option, Setter<String> setter)
				throws IOException {
			super(parser, option, setter, "classpath*:/META-INF/spring-xd/transports/*-admin.xml",
					SINGLE_NODE_SPECIAL_CONTROL_TRANSPORT);
		}

	}

	/**
	 * Computes values for --controlTransport for the singlenode case.
	 */
	public static class SingleNodeControlTransportOptionHandler extends FromResourceLocationOptionHandler {

		public SingleNodeControlTransportOptionHandler(CmdLineParser parser, OptionDef option, Setter<String> setter)
				throws IOException {
			super(parser, option, setter, "classpath*:/META-INF/spring-xd/transports/*-admin.xml");
		}

	}

	/**
	 * Computes values for (data) --transport for the distributed case.
	 */
	public static class DistributedDataTransportOptionHandler extends FromResourceLocationOptionHandler {

		public DistributedDataTransportOptionHandler(CmdLineParser parser, OptionDef option, Setter<String> setter)
				throws IOException {
			super(parser, option, setter, "classpath*:/META-INF/spring-xd/transports/*-bus.xml", "local");
		}

	}

	/**
	 * Computes values for (data) --transport for the singlenode case.
	 */
	public static class SingleNodeDataTransportOptionHandler extends FromResourceLocationOptionHandler {

		public SingleNodeDataTransportOptionHandler(CmdLineParser parser, OptionDef option, Setter<String> setter)
				throws IOException {
			super(parser, option, setter, "classpath*:/META-INF/spring-xd/transports/*-bus.xml");
		}

	}

	/**
	 * Computes values for --analytics in the single node case (accepts memory).
	 */
	public static class SingleNodeAnalyticsOptionHandler extends FromResourceLocationOptionHandler {

		public SingleNodeAnalyticsOptionHandler(CmdLineParser parser, OptionDef option, Setter<String> setter)
				throws IOException {
			super(parser, option, setter, "classpath*:/META-INF/spring-xd/analytics/*-analytics.xml");
		}

	}

	/**
	 * Computes values for --analytics in the distributed case (memory is NOT supported).
	 */
	public static class DistributedAnalyticsOptionHandler extends FromResourceLocationOptionHandler {

		public DistributedAnalyticsOptionHandler(CmdLineParser parser, OptionDef option, Setter<String> setter)
				throws IOException {
			super(parser, option, setter, "classpath*:/META-INF/spring-xd/analytics/*-analytics.xml", "memory");
		}

	}

	/**
	 * Computes values for --store in the distributed case (memory is NOT supported).
	 */
	public static class StoreOptionHandler extends FromResourceLocationOptionHandler {

		public StoreOptionHandler(CmdLineParser parser, OptionDef option, Setter<String> setter)
				throws IOException {
			super(parser, option, setter, "classpath*:/META-INF/spring-xd/store/*-store.xml");
		}

	}


}
