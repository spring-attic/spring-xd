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

import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;


/**
 * A standard mixin for modules that do some transformation based on either a script or a SpEL expression.
 *
 * <p>
 * Provides the following options:
 * <ul>
 * <li>expression</li>
 * </ul>
 * and activates one of the following profile accordingly:
 * <ul>
 * <li>use-script</li>
 * <li>use-expression</li>
 * </ul>
 *
 * @author Eric Bottard
 * @author David Turanski
 */
public class ExpressionOrScriptMixin extends ScriptMixin implements ProfileNamesProvider {
	/**
	 * The default expression if none is provided.
	 */
	private static final String DEFAULT_EXPRESSION = "payload.toString()";

	private String expression;

	public String getExpression() {
		return expression == null ? DEFAULT_EXPRESSION : expression;
	}

	@ModuleOption("a SpEL expression used to transform messages")
	public void setExpression(String expression) {
		this.expression = expression;
	}

	/**
	 * User can't explicitly set both script and expression.
	 */
	@Override
	@AssertTrue(message = "the 'script' and 'expression' options are mutually exclusive")
	public boolean isValid() {
		// default value for expression is always set; this is safe as profilesToActivate() will activate 'use-script'
		// if script is non-null
		return getScript() == null || expression.equals(DEFAULT_EXPRESSION);
	}

	@AssertTrue(message="'propertiesLocation' and 'variables' only apply to script")
	public boolean noScriptOptionsWithExpression() {
		return getScript() == null ^ (getPropertiesLocation() == null && getVariables() == null);
	}

	@Override
	public String[] profilesToActivate() {
		return getScript() == null ? new String[] { "use-expression" } : new String[] { "use-script" };
	}
}
