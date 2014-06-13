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

package org.springframework.xd.test.fixtures;

import org.springframework.util.Assert;


/**
 * Test fixture that creates a Transform.
 * @author Glenn Renfro
 */
public class Transform extends AbstractModuleFixture {

	private String expression;

	private String script;

	private String properties;

	private String label;

	/**
	 * Renders the default DSL for this fixture.
	 */
	@Override
	protected String toDSL() {
		StringBuilder dsl = new StringBuilder();
		if (label != null) {
			dsl.append(" ");
			dsl.append(label);
			dsl.append(": ");
		}

		dsl.append("transform");

		if (expression != null) {
			dsl.append(" --expression=");
			dsl.append(expression);
		}
		else if (script != null) {
			dsl.append(" --script=");
			dsl.append(script);
		}
		if (properties != null) {
			dsl.append(" properties=" + properties);
		}

		return dsl.toString();
	}

	/**
	 * Sets the expression to be used by the transform
	 * @param expression The expression to transform the input
	 * @return current instance of the transform fixture.
	 */
	public Transform expression(String expression) {
		Assert.hasText(expression, "expression can not be empty nor null");
		this.expression = expression;
		return this;
	}

	/**
	 Sets the groovy script the user wants to transform the input.
	 * @param The file where the groovy script is located.
	 * @return current instance of the transform fixture.
	 */
	public Transform moduleName(String script) {
		Assert.hasText(script, "script can not be empty nor null");
		this.script = script;
		return this;
	}

	/**
	 * Sets the properties to be used by the transform.
	 * @param The location of the properties file 
	 * @return current instance of the transform fixture.
	 */
	public Transform properties(String properties) {
		Assert.hasText(properties, "properties can not be empty nor null");
		this.properties = properties;
		return this;
	}

	/**
	 * Sets the label for this instance of transform
	 * @param label The label to be used by this transform.
	 * @return current instance of the transform fixture
	 */
	public Transform label(String label) {
		Assert.hasText(label, "label can not be empty nor null");
		this.label = label;
		return this;
	}

}
