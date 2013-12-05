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

package org.springframework.xd.dirt.modules.metadata;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;

import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;
import org.springframework.xd.module.options.spi.SourceModuleOptionsMetadataSupport;

/**
 * Describes options to the {@code trigger} source module.
 * 
 * @author Eric Bottard
 */
public class TriggerSourceOptionsMetadata extends SourceModuleOptionsMetadataSupport implements ProfileNamesProvider {

	private Integer fixedDelay;

	private String cron;

	private String payload = "";

	@Override
	public String[] profilesToActivate() {
		if (cron != null) {
			return new String[] { "use-cron" };
		}
		else if (fixedDelay != null) {
			return new String[] { "use-delay" };
		}
		else {
			return new String[] { "use-date" };
		}
	}

	@Min(0)
	public Integer getFixedDelay() {
		return fixedDelay;
	}

	@AssertTrue(message = "cron and fixedDelay are mutually exclusive")
	private boolean isValid() {
		return !(fixedDelay != null && cron != null);
	}

	@ModuleOption("number of seconds between executions")
	public void setFixedDelay(Integer fixedDelay) {
		this.fixedDelay = fixedDelay;
	}


	public String getCron() {
		return cron;
	}

	@ModuleOption("cron expression specifying when the trigger should fire")
	public void setCron(String cron) {
		this.cron = cron;
	}


	public String getPayload() {
		return payload;
	}

	@ModuleOption("the message that will be sent when the trigger fires")
	public void setPayload(String payload) {
		this.payload = payload;
	}


}
