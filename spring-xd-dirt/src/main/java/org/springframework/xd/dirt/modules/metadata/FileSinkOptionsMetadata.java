/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.modules.metadata;

import static org.springframework.xd.dirt.modules.metadata.FileSinkOptionsMetadata.Mode.APPEND;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import org.springframework.util.StringUtils;
import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * 
 * Holds options to the 'file' sink module.
 */
public class FileSinkOptionsMetadata {

	private boolean binary = false;

	private String charset = "UTF-8";

	private String dir = "/tmp/xd/output/";

	// Don't provide default here but in xml file for now
	private String name;

	private String suffix = "out";

	private Mode mode = APPEND;

	@NotNull
	public Mode getMode() {
		return mode;
	}

	@ModuleOption("what to do if the file already exists")
	public void setMode(Mode mode) {
		this.mode = mode;
	}

	/**
	 * Return dot + suffix if suffix is set, or the empty string otherwise.
	 */
	public String getExtensionWithDot() {
		return StringUtils.hasText(suffix) ? "." + suffix.trim() : "";
	}


	@ModuleOption("filename extension to use")
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public String getName() {
		return name;
	}

	@ModuleOption("filename pattern to use")
	public void setName(String name) {
		this.name = name;
	}

	@NotBlank
	public String getDir() {
		return dir;
	}

	@ModuleOption("the directory in which files will be created")
	public void setDir(String dir) {
		this.dir = dir;
	}

	public boolean isBinary() {
		return binary;
	}

	@ModuleOption("if false, will append a newline character at the end of each line")
	public void setBinary(boolean binary) {
		this.binary = binary;
	}

	@ModuleOption("the charset to use when writing a String payload")
	public void setCharset(String charset) {
		this.charset = charset;
	}

	@NotBlank
	public String getCharset() {
		return charset;
	}

	public static enum Mode {
		APPEND, REPLACE, FAIL, IGNORE;
	}

}
