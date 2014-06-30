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

package org.springframework.xd.module.options.spi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Used to annotate the setter methods of a POJO Module Options class to provide the framework with additional option
 * information.
 * 
 * @author Eric Bottard
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ModuleOption {

	public static final String NO_DEFAULT = "__NULL__";

	/**
	 * Provide a short description of the option.
	 */
	String value();

	/**
	 * Provide a default value representation of the option. Only needed when there is no corresponding getter for that
	 * option that would return the default from a newly created instance.
	 */
	String defaultValue() default NO_DEFAULT;

	/**
	 * Whether this option is considered "expert" or "infrastructure" and should typically not be presented to user.
	 */
	boolean hidden() default false;


}
