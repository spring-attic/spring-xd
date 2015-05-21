/*
 * Copyright 2014-2015 the original author or authors.
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
import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_STREAM_NAME;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import org.springframework.util.StringUtils;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;

/**
 *
 * Holds options to the 'file' sink module.
 *
 * @author Eric Bottard
 * @author Franck Marchand
 */
public class FileSinkOptionsMetadata implements ProfileNamesProvider {

	private static final String USE_SPEL_PROFILE = "use-expression";

	private static final String USE_LITERAL_STRING_PROFILE = "use-string";

	private boolean binary = false;

	private String charset = "UTF-8";

	private String dir = "/tmp/xd/output/";

	private String name = XD_STREAM_NAME;

	private String suffix = "out";

	private Mode mode = APPEND;

	private String nameExpression;

	private String dirExpression;

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


	public String getNameExpression() {
		return nameExpression;
	}

	@ModuleOption("spring expression used to define filename")
	public void setNameExpression(String nameExpression) {
		this.nameExpression = nameExpression;
	}

	public String getDirExpression() {
		return dirExpression;
	}

	@ModuleOption("spring expression used to define directory name")
	public void setDirExpression(String dirExpression) {
		this.dirExpression = dirExpression;
	}

	public static enum Mode {
		APPEND, REPLACE, FAIL, IGNORE;
	}

	@Override
	public String[] profilesToActivate() {
		return (nameExpression != null || dirExpression != null) ? new String[] { USE_SPEL_PROFILE }
				: new String[] { USE_LITERAL_STRING_PROFILE };
	}
}
