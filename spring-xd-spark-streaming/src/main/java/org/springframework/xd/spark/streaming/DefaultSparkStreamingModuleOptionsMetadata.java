/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.spark.streaming;

import javax.validation.constraints.AssertTrue;

import org.apache.spark.storage.StorageLevel;

import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Default module options for {@link org.springframework.xd.spark.streaming.SparkStreamingSupport}.
 * Both the java and scala based implementations can extend this.
 *
 * @author Ilayaperumal Gopinathan
 */
public class DefaultSparkStreamingModuleOptionsMetadata {

	private String moduleExecutionFramework = SparkStreamingSupport.MODULE_EXECUTION_FRAMEWORK;

	private String batchInterval;

	private String storageLevel = "";

	private boolean enableTap = false;

	@ModuleOption("the time interval in millis for batching the stream events")
	public void setBatchInterval(final String batchInterval) {
		this.batchInterval = batchInterval;
	}

	public String getBatchInterval() {
		return this.batchInterval;
	}

	@ModuleOption("the storage level for spark streaming RDD persistence")
	public void setStorageLevel(final String storageLevel) {
		this.storageLevel = storageLevel;
	}

	public String getStorageLevel() {
		return this.storageLevel;
	}

	@AssertTrue(message = "Use a valid storage level option. See org.apache.spark.storage.StorageLevel")
	public boolean isSparkStorageValid() {
		if (storageLevel.length() == 0) {
			return true;
		}
		else {
			try {
				StorageLevel.fromString(storageLevel);
				return true;
			}
			catch (IllegalArgumentException e) {
				return false;
			}
		}
	}

	@ModuleOption("enable tap at the output of the spark processor module")
	public void setEnableTap(boolean enableTap) {
		this.enableTap = enableTap;
	}

	public boolean isEnableTap() {
		return this.enableTap;
	}

	@ModuleOption(value = "the underlying execution framework", hidden = true)
	public String getModuleExecutionFramework() {
		return this.moduleExecutionFramework;
	}
}
