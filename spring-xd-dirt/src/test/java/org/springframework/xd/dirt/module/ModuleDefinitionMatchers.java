/*
 *
 *  * Copyright 2011-2014 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.xd.dirt.module;

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;

import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

/**
 * Hamcrest matchers for ModuleDefinition.
 *
 * @author Eric Bottard
 */
public class ModuleDefinitionMatchers {

	/**
	 * Return a matcher that matches a definition by the given name and type.
	 */
	public static Matcher<ModuleDefinition> module(final String name, final ModuleType type) {
		return new DiagnosingMatcher<ModuleDefinition>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("a module named ").appendValue(name).appendText(" with type ").appendValue(type);
			}

			@Override
			protected boolean matches(Object item, Description mismatchDescription) {
				if (item == null) {
					return false;
				}
				ModuleDefinition def = (ModuleDefinition) item;
				mismatchDescription.appendText("a module named ").appendValue(def.getName())
						.appendText(" with type ").appendValue(def.getType());
				return name.equals(def.getName()) && type == def.getType();
			}
		};
	}

}
