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

package org.springframework.xd.dirt.server.options;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

/**
 * A {@link PropertySource} that exposes the javabeans properties of an object as properties.
 * 
 * @author Eric Bottard
 */
public class BeanPropertiesPropertySource<T> extends EnumerablePropertySource<T> {

	private final BeanWrapper beanWrapper;

	public BeanPropertiesPropertySource(String name, T source) {
		super(name, source);
		beanWrapper = new BeanWrapperImpl(source);
	}


	@Override
	public String[] getPropertyNames() {
		List<String> result = new ArrayList<String>();
		for (PropertyDescriptor pd : beanWrapper.getPropertyDescriptors()) {
			String name = pd.getName();
			if (beanWrapper.isReadableProperty(name) && !"class".equals(name)) {
				result.add(name);
			}
		}
		return result.toArray(new String[result.size()]);
	}

	@Override
	public Object getProperty(String name) {
		if (Arrays.asList(getPropertyNames()).contains(name)) {
			return beanWrapper.getPropertyValue(name);
		}
		else {
			return null;
		}
	}

}
