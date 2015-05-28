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

package org.springframework.xd.extension.process;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import org.springframework.xd.module.options.mixins.FromStringCharsetMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.tcp.encdec.EncoderDecoderMixins.BufferSizeMixin;
import org.springframework.xd.tcp.encdec.EncoderDecoderMixins.EncoderMixin;

/**
 * Options Metadata for shell processor and sink modules.
 * @author David Turanski
 */
@Mixin({ EncoderMixin.class,
	BufferSizeMixin.class,
	FromStringCharsetMixin.class })
public class ShellModuleOptionsMetadata {


	private String command;

	private String workingDir;

	private boolean redirectErrorStream;

	private String environment;

	@ModuleOption("additional process environment variables as comma delimited name-value pairs")
	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public String getEnvironment() {
		return environment;
	}

	@NotEmpty
	@NotNull
	public String getCommand() {
		return command;
	}

	@ModuleOption("the shell command")
	public void setCommand(String command) {
		this.command = command;
	}

	public String getWorkingDir() {
		return workingDir;
	}

	@ModuleOption("the process working directory")
	public void setWorkingDir(String workingDir) {
		this.workingDir = workingDir;
	}

	public boolean isRedirectErrorStream() {
		return redirectErrorStream;
	}

	@ModuleOption("redirects stderr to stdout")
	public void setRedirectErrorStream(boolean redirectErrorStream) {
		this.redirectErrorStream = redirectErrorStream;
	}
}
