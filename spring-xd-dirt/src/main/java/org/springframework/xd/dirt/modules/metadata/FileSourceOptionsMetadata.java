/*
 * Copyright 2015 the original author or authors.
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
 *
 *
 */

package org.springframework.xd.dirt.modules.metadata;

import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotBlank;

import org.springframework.xd.module.options.mixins.MaxMessagesDefaultUnlimitedMixin;
import org.springframework.xd.module.options.mixins.PeriodicTriggerMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ModulePlaceholders;

/**
 * Holds module options metadata about the {@code file} source.
 *
 * @author Eric Bottard
 * @author Gary Russell
 */

@Mixin({ FileAsRefMixin.class, PeriodicTriggerMixin.class, MaxMessagesDefaultUnlimitedMixin.class })
public class FileSourceOptionsMetadata {

	private String dir = "/tmp/xd/input/" + ModulePlaceholders.XD_STREAM_NAME;

	private boolean preventDuplicates = true;

	private String pattern = "*";

	private int fixedDelay = 5;


	@Min(0)
	public int getFixedDelay() {
		return fixedDelay;
	}

	@ModuleOption("the fixed delay polling interval specified in seconds")
	public void setFixedDelay(int fixedDelay) {
		this.fixedDelay = fixedDelay;
	}

	@NotBlank
	public String getDir() {
		return dir;
	}

	@ModuleOption("the absolute path to the directory to monitor for files")
	public void setDir(String dir) {
		this.dir = dir;
	}

	public boolean isPreventDuplicates() {
		return preventDuplicates;
	}

	@ModuleOption("whether to prevent the same file from being processed twice")
	public void setPreventDuplicates(boolean preventDuplicates) {
		this.preventDuplicates = preventDuplicates;
	}

	@NotBlank
	public String getPattern() {
		return pattern;
	}

	@ModuleOption("a filter expression (Ant style) to accept only files that match the pattern")
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
}
