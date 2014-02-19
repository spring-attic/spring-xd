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

import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Describes options to the {@code tail} source module.
 * 
 * @author Eric Bottard
 */
public class TailSourceOptionsMetadata {

	private int lines = 0;

	private String name;

	private String nativeOptions;

	private long fileDelay = 5000L;

	// Use wrapper types to track not set values
	private Long delay = null;

	private Boolean fromEnd = null;

	private Boolean reOpen = null;

	// No getter, as this is not surfaced directly in tail.xml
	// see getNativeOptions()
	@ModuleOption(value = "the number of lines prior to the end of an existing file to tail; does not apply if 'nativeOptions' is provided", defaultValue = "0")
	public void setLines(int lines) {
		this.lines = lines;
	}


	public String getName() {
		return name;
	}

	@ModuleOption("the absolute path of the file to tail")
	public void setName(String name) {
		this.name = name;
	}

	public String getNativeOptions() {
		if (nativeOptions != null) {
			return nativeOptions;
		} // Only return the default if not using Apache impl.
		else {
			return usingApache() ? null : String.format("-F -n %d", lines);
		}
	}

	private boolean usingApache() {
		return delay != null || fromEnd != null || reOpen != null;
	}

	private boolean usingNative() {
		return nativeOptions != null;
	}

	@AssertTrue(message = "nativeOptions and Apache specific options are mutually exclusive")
	private boolean isValid() {
		return !(usingApache() && usingNative());
	}

	@ModuleOption("options for a native tail command; do not set and use 'end', 'delay', and/or 'reOpen' to use the Apache Tailer")
	public void setNativeOptions(String nativeOptions) {
		this.nativeOptions = nativeOptions;
	}


	public long getFileDelay() {
		return fileDelay;
	}

	@ModuleOption("on platforms that don't wait for a missing file to appear, how often (ms) to look for the file")
	public void setFileDelay(long fileDelay) {
		this.fileDelay = fileDelay;
	}


	public Long getDelay() {
		return delay;
	}

	@ModuleOption("how often (ms) to poll for new lines (forces use of the Apache Tailer, requires nativeOptions='')")
	public void setDelay(long delay) {
		this.delay = delay;
	}


	public Boolean getFromEnd() {
		return fromEnd;
	}


	@ModuleOption("whether to tail from the end (true) or from the start (false) of the file (forces use of the Apache Tailer, requires nativeOptions='')")
	public void setFromEnd(boolean fromEnd) {
		this.fromEnd = fromEnd;
	}


	public Boolean getReOpen() {
		return reOpen;
	}

	@ModuleOption("whether to reopen the file each time it is polled (forces use of the Apache Tailer, requires nativeOptions='')")
	public void setReOpen(boolean reOpen) {
		this.reOpen = reOpen;
	}


}
