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

import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertiesPropertySource;


/**
 * Implementation of {@link ModuleOptions} to be used when no explicit configuration has been provided.
 * 
 * Returned {@link InterpolatedModuleOptions} will simply reflect the underlying properties object. Moreover, validation
 * and profile selection is not supported.
 * 
 * @author Eric Bottard
 */
public class AbsentModuleOptions implements ModuleOptions {

	public static final AbsentModuleOptions INSTANCE = new AbsentModuleOptions();

	private AbsentModuleOptions() {

	}

	@Override
	public Iterator<ModuleOption> iterator() {
		return Collections.<ModuleOption> emptyList().iterator();
	}

	@Override
	public InterpolatedModuleOptions interpolate(final Properties raw) {
		return new InterpolatedModuleOptions() {

			@Override
			public EnumerablePropertySource<?> asPropertySource() {
				return new PropertiesPropertySource(this.toString(), raw);
			}
		};
	}

}
