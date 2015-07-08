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

package org.springframework.xd.sqoop;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.xd.jdbc.JdbcConnectionMixin;
import org.springframework.xd.module.options.mixins.HadoopConfigurationMixin;
import org.springframework.xd.module.options.mixins.MapreduceConfigurationMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Module options for Sqoop application module.
 *
 * @author Thomas Risberg
 */
@Mixin({ JdbcConnectionMixin.class, HadoopConfigurationMixin.class, MapreduceConfigurationMixin.class })
public class SqoopOptionsMetadata {

	private String command = "";

	private String args = "";

	private String libjars = "";

	@NotBlank
	public String getCommand() {
		return command;
	}

	@ModuleOption("the Sqoop command to run")
	public void setCommand(String command) {
		this.command = command;
	}

	public String getArgs() {
		return args;
	}

	@ModuleOption("the arguments for the Sqoop command")
	public void setArgs(String args) {
		this.args = args;
	}

	public String getLibjars() {
		return libjars;
	}

	@ModuleOption("extra jars from the classpath to add to the job")
	public void setLibjars(String libjars) {
		this.libjars = libjars;
	}
}
