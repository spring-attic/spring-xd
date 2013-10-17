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

import org.springframework.core.env.PropertySource;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;


/**
 * A SpEL {@link PropertyAccessor} that knows how to extract values out of a {@link PropertySource}.
 * 
 * @author Eric Bottard
 */
public class PropertySourcePropertyAccessor implements PropertyAccessor {

	private static final Class[] SUPPORTED_CLASSES = new Class[] { PropertySource.class };

	@Override
	public Class[] getSpecificTargetClasses() {
		return SUPPORTED_CLASSES;
	}

	@Override
	public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
		return ((PropertySource) target).containsProperty(name);
	}

	@Override
	public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
		return new TypedValue(((PropertySource) target).getProperty(name));
	}

	@Override
	public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
		return false;
	}

	@Override
	public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
		throw new AssertionError("Should never be called given canWrite()");
	}

}
