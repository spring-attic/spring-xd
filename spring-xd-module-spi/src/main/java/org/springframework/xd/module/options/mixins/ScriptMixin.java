/*
 * Copyright 2014-2016 the original author or authors.
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

import org.springframework.util.StringUtils;
import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * A standard mixin for modules that do some transformation based on a script.
 * Provides the following options:
 * <ul>
 *     <li>script</li>
 *     <li>propertiesLocation</li>
 *     <li>variables</li>
 * </ul>
 *
 * @author David Turanski
 * @author Gary Russell
 */
public class ScriptMixin {

	private String script = null;

	private String propertiesLocation;

	private String variables;

	private long refreshDelay = 60000;

	public String getVariables() {
		return variables;
	}

	public String getScript() {
		return script;
	}

	public String getPropertiesLocation() {
		return propertiesLocation;
	}

	@ModuleOption("reference to a script used to process messages")
	public void setScript(String script) {
		this.script = script;
	}

	@ModuleOption("the path of a properties file containing custom script variable bindings")
	public void setPropertiesLocation(String propertiesLocation) {
		this.propertiesLocation = propertiesLocation;
	}

	@ModuleOption("variable bindings as a comma delimited string of name-value pairs, e.g., 'foo=bar,baz=car'")
	public void setVariables(String variables) {
		this.variables = variables;
	}


	public long getRefreshDelay() {
		return refreshDelay;
	}

	@ModuleOption("how often to check (in milliseconds) whether the script has changed; -1 for never")
	public void setRefreshDelay(long refreshDelay) {
		this.refreshDelay = refreshDelay;
	}

	/**
	 *
	 * Validates the configuration meets expected conditions. Subclasses may override validation rules.
	 * @return true if configuration is valid.
	 */
	@AssertTrue(message = "'script' cannot be null or empty") //Allows subclasses to override 'script' is required.
	public boolean isValid() {
		return StringUtils.hasText(script);
	}

}
