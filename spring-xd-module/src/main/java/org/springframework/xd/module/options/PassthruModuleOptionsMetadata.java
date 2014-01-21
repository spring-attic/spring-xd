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

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.validation.BindException;

/**
 * Implementation of {@link ModuleOptionsMetadata} used when no explicit information about a module has been authored.
 * 
 * This implementation does not know how to list its module options and will pass thru the raw options data it receives
 * as options.
 * 
 * @author Eric Bottard
 */
public class PassthruModuleOptionsMetadata implements ModuleOptionsMetadata {

	@Override
	public Iterator<ModuleOption> iterator() {
		return Collections.<ModuleOption> emptyList().iterator();
	}

	@Override
	public ModuleOptions interpolate(final Map<String, String> raw) throws BindException {
		return new ModuleOptions() {

			@Override
			@SuppressWarnings("unchecked")
			public EnumerablePropertySource<?> asPropertySource() {
				String uniqueName = String.format("%s-%s", PassthruModuleOptionsMetadata.class.getSimpleName(),
						System.identityHashCode(PassthruModuleOptionsMetadata.this));
				return new MapPropertySource(uniqueName, (Map) raw);
			}
		};
	}

}
