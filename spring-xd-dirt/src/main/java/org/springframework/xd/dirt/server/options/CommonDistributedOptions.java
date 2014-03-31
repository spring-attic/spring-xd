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

import javax.validation.constraints.NotNull;

import org.kohsuke.args4j.Option;

import org.springframework.xd.dirt.server.options.ResourcePatternScanningOptionHandlers.DistributedAnalyticsOptionHandler;


/**
 * Holds options that are common to both admin and container servers, when used in distributed mode. Note that single
 * node has its own options class, because valid values are different.
 *
 * @author Eric Bottard
 */
public class CommonDistributedOptions extends CommonOptions {

	@Option(name = "--analytics", handler = DistributedAnalyticsOptionHandler.class,
			usage = "How to persist analytics such as counters and gauges")
	private String analytics;

	@NotNull
	public String getXD_ANALYTICS() {
		return analytics;
	}

	public void setXD_ANALYTICS(String analytics) {
		this.analytics = analytics;
	}

}
