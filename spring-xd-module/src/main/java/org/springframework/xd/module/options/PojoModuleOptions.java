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

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.env.EnumerablePropertySource;


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
 * {@link ProfileNamesProvider#profilesToActivate()}</li>
 * </ul>
 * 
 * @author Eric Bottard
 */
public class PojoModuleOptions implements ModuleOptions {

	private BeanWrapper beanWrapper;

	private List<ModuleOption> options;

	public PojoModuleOptions(Class<?> clazz) {
		beanWrapper = new BeanWrapperImpl(clazz);
		options = new ArrayList<ModuleOption>();
		for (PropertyDescriptor pd : beanWrapper.getPropertyDescriptors()) {
			String name = pd.getName();
			if (!beanWrapper.isWritableProperty(name)) {
				continue;
			}
			ModuleOption option = new ModuleOption(name, "TODO").withType(pd.getPropertyType());
			if (beanWrapper.isReadableProperty(name)) {
				option.withDefaultValue(beanWrapper.getPropertyValue(name));
			}
			options.add(option);
		}
	}

	public static void main(String[] args) {
		PojoModuleOptions pojoModuleOptions = new PojoModuleOptions(TriggerModuleOptions.class);
		for (ModuleOption o : pojoModuleOptions) {
			System.out.println(o);
		}
	}

	@Override
	public Iterator<ModuleOption> iterator() {
		return options.iterator();
	}

	@Override
	public InterpolatedModuleOptions interpolate(Properties raw) {
		beanWrapper.setPropertyValues(new MutablePropertyValues(raw), true, true);
		return new InterpolatedModuleOptions() {

			@Override
			public EnumerablePropertySource<?> asPropertySource() {
				return new EnumerablePropertySource<BeanWrapper>(this.toString(), beanWrapper) {

					@Override
					public String[] getPropertyNames() {
						List<String> result = new ArrayList<String>();
						for (PropertyDescriptor pd : beanWrapper.getPropertyDescriptors()) {
							String name = pd.getName();
							if (beanWrapper.isReadableProperty(name)) {
								result.add(name);
							}
						}
						return result.toArray(new String[result.size()]);
					}

					@Override
					public Object getProperty(String name) {
						return beanWrapper.getPropertyValue(name);
					}
				};
			}

			@Override
			public String[] profilesToActivate() {
				if (beanWrapper.getWrappedInstance() instanceof ProfileNamesProvider) {
					return ((ProfileNamesProvider) beanWrapper.getWrappedInstance()).profilesToActivate();
				}
				else {
					return super.profilesToActivate();
				}
			}
		};
	}
}
