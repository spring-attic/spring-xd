/*
 * Copyright 2013 the original author or authors.
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

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;


/**
 * Represents runtime information about a module once user provided values are known. Depending on actual
 * implementation, ModuleOptions may be able to
 * <ul>
 * <li>return the value of a property the user provided</li>
 * <li>return the value of a property derived from other properties</li>
 * <li>validate that the set of properties given by the user is consistent</li>
 * <li>return a list of profile names that should be activated, most certainly computed from the values the user
 * provided</li>
 * </ul>
 * 
 * @author Eric Bottard
 */
public abstract class ModuleOptions implements ProfileNamesProvider {

	private static final String[] NONE = new String[0];

	public abstract EnumerablePropertySource<?> asPropertySource();

	@Override
	public String[] profilesToActivate() {
		return NONE;
	}

	public void validate() {

	}

}
