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

import java.util.Iterator;
import java.util.Properties;


/**
 * An implementation of {@link ModuleOptions} that derives its information from a plain old java object:
 * <ul>
 * <li>public setters are reported as valid options</li>
 * <li>the type of the option is derived from the accepted type by the setter</li>
 * </ul>
 * 
 * {@link InterpolatedModuleOptions} for such a POJO will work as follows:
 * <ul>
 * <li>an instance of the class will be created reflectively and injected with user provided values,</li>
 * <li>reported properties are computed from all the getters,</li>
 * <li>the POJO may bear JSR303 validation annotations, which will be used to validate the interpolated options,</li>
 * <li>if the POJO implements {@link ProfileNamesProvider}, profile names will be gathered from a reflective call to
 * {@link ProfileNamesProvider#getProfilesToActivate()}</li>
 * </ul>
 * 
 * @author Eric Bottard
 */
public class PojoModuleOptions implements ModuleOptions {

	private final Class<?> clazz;

	public PojoModuleOptions(Class<?> clazz) {
		this.clazz = clazz;
	}

	@Override
	public Iterator<ModuleOption> iterator() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public InterpolatedModuleOptions interpolate(Properties raw) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
