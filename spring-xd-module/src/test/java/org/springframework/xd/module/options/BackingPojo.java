/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.module.options;

import javax.validation.constraints.Max;
import javax.validation.groups.Default;

import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;
import org.springframework.xd.module.options.spi.ValidationGroupsProvider;

class BackingPojo implements ProfileNamesProvider, ValidationGroupsProvider {

	private String foo = "somedefault";

	private static interface High extends Default {
	}

	@Max(10000)
	private int bar = 42;

	@Max(groups = High.class, value = 3, message = "length must be < 3 when bar > 2000")
	public String getFoo() {
		return foo;
	}


	@ModuleOption("sets the foo option")
	public void setFoo(String foo) {
		this.foo = foo;
	}

	@ModuleOption(value = "sets the bar option", defaultValue = "42")
	public void setBar(int bar) {
		this.bar = bar;
	}

	public String getFooBar() {
		return foo + bar;
	}


	@Override
	public String[] profilesToActivate() {
		if (bar > 50) {
			return new String[] { "high-profile" };
		}
		else {
			return new String[] {};
		}
	}


	@Override
	public Class<?>[] groupsToValidate() {
		if (bar > 2000) {
			return new Class<?>[] { High.class };
		}
		else {
			return DEFAULT_GROUP;
		}
	}

}
