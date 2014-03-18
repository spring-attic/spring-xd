/*
 * Copyright 2013-2014 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.xd.integration.throughput;

import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * @author Jon Brisbin
 */
public class ThroughputSamplerOptionsMetadata {

	private String startMessage = "START";

	private String endMessage = "END";

	private String sampleUnit = "seconds";

	public String getStartMessage() {
		return startMessage;
	}

	@ModuleOption("the message payload to demarcate the start of a sampling")
	public void setStartMessage(String startMessage) {
		this.startMessage = startMessage;
	}

	public String getEndMessage() {
		return endMessage;
	}

	@ModuleOption("the message payload to demarcate the end of a sampling")
	public void setEndMessage(String endMessage) {
		this.endMessage = endMessage;
	}

	public String getSampleUnit() {
		return sampleUnit;
	}

	@ModuleOption("the TimeUnit in which throughput measurements are to be reported")
	public void setSampleUnit(String sampleUnit) {
		this.sampleUnit = sampleUnit;
	}

}
