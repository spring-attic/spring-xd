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
import org.springframework.xd.test.fixtures.util.FixtureUtils;


/**
 * Test fixture that creates a Transform.
 * @author Glenn Renfro
 * @author David Turanski
 */
public class Transform extends AbstractModuleFixture<Transform> {

	private String expression;

	private String script;

	private String propertiesLocation;

	/**
	 * Renders the default DSL for this fixture.
	 */
	@Override
	protected String toDSL() {
		StringBuilder dsl = new StringBuilder();
		dsl.append(FixtureUtils.labelOrEmpty(label));

		dsl.append("transform");

		if (expression != null) {
			dsl.append(" --expression=");
			dsl.append(expression);
		}
		else if (script != null) {
			dsl.append(" --script=");
			dsl.append(script);
		}
		if (propertiesLocation != null) {
			dsl.append(" --propertiesLocation =" + propertiesLocation);
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
	 * Sets the groovy script the user wants to transform the input.
	 * @param script the file where the groovy script is located.
	 * @return current instance of the transform fixture.
	 */
	public Transform moduleName(String script) {
		Assert.hasText(script, "script can not be empty nor null");
		this.script = script;
		return this;
	}

	/**
	 * Sets the propertiesLocation to be used by the transform.
	 * @param propertiesLocation the location of the propertiesLocation file
	 * @return current instance of the transform fixture.
	 */
	public Transform properties(String propertiesLocation) {
		Assert.hasText(propertiesLocation, "propertiesLocation can not be empty nor null");
		this.propertiesLocation = propertiesLocation;
		return this;
	}

}
