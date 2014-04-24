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

package org.springframework.xd.module.options;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;

import org.hamcrest.Matcher;


/**
 * Contains hamcrest matchers and utilities related to module options.
 * 
 * @author Eric Bottard
 */
public class ModuleOptionMatchers {

	public static Matcher<ModuleOption> moduleOptionNamed(String name) {
		return hasProperty("name", equalTo(name));
	}

	public static <T> Matcher<ModuleOption> moduleOptionWithDefaultValue(Matcher<T> matcher) {
		return hasProperty("defaultValue", matcher);
	}

}
