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
import org.springframework.util.Assert;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;


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
			org.springframework.xd.module.options.spi.ModuleOption annotation = pd.getWriteMethod().getAnnotation(
					org.springframework.xd.module.options.spi.ModuleOption.class);
			Assert.notNull(
					annotation,
					String.format(
							"Setter method for option '%s' needs to bear the @%s annotation and provide a 'description'",
							name,
							org.springframework.xd.module.options.spi.ModuleOption.class.getSimpleName()));

			String description = descriptionFromAnnotation(name, annotation);
			ModuleOption option = new ModuleOption(name, description).withType(pd.getPropertyType());
			if (beanWrapper.isReadableProperty(name)) {
				option.withDefaultValue(beanWrapper.getPropertyValue(name));
			}
			else {
				option.withDefaultValue(defaultFromAnnotation(annotation));
			}
			options.add(option);
		}
	}

	private Object defaultFromAnnotation(org.springframework.xd.module.options.spi.ModuleOption annotation) {
		String value = annotation.defaultValue();
		return org.springframework.xd.module.options.spi.ModuleOption.NO_DEFAULT.equals(value) ? null : value;
	}

	/**
	 * Read the 'description' attribute from the annotation on the getter.
	 */
	private String descriptionFromAnnotation(String optionName,
			org.springframework.xd.module.options.spi.ModuleOption annotation) {
		Assert.hasLength(
				annotation.value(),
				String.format(
						"Setter method for option '%s' needs to bear the @%s annotation and provide a non-empty 'description' attribute",
						optionName,
						org.springframework.xd.module.options.spi.ModuleOption.class.getSimpleName()));
		return annotation.value();
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
