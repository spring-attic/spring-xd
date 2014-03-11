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

package org.springframework.xd.module.options.mixins;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * A mixin for configuring a typical simple retry + exponential backoff Spring Retry combination. Defines the following
 * options:
 * <ul>
 * <li>retryMultiplier (2)
 * <li>retryInitialInterval (2000)</li>
 * <li>retryMaxAttempts (5)</li>
 * </ul>
 * 
 * @author Eric Bottard
 */
public class ExponentialBackoffRetryOptionsMetadataMixin {

	private double retryMultiplier = 2.0D;

	private long retryInitialInterval = 2000;

	private int retryMaxAttempts = 5;

	public double getRetryMultiplier() {
		return retryMultiplier;
	}

	@AssertTrue(message = "retryMultiplier must be greater than 1.0")
	private boolean isRetryMultiplierValid() {
		return retryMultiplier >= 1.0D;
	}

	@Min(0)
	public long getRetryInitialInterval() {
		return retryInitialInterval;
	}

	@Min(1)
	public int getRetryMaxAttempts() {
		return retryMaxAttempts;
	}

	@ModuleOption("the multiplier for exponential back off of retries")
	public void setRetryMultiplier(double retryMultiplier) {
		this.retryMultiplier = retryMultiplier;
	}

	@ModuleOption("the time (ms) to wait for the first retry")
	public void setRetryInitialInterval(long retryInitialInterval) {
		this.retryInitialInterval = retryInitialInterval;
	}

	@ModuleOption("the maximum number of attempts")
	public void setRetryMaxAttempts(int retryMaxAttempts) {
		this.retryMaxAttempts = retryMaxAttempts;
	}


}
