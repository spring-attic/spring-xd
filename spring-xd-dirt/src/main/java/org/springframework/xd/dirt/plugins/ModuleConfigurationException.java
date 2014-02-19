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

package org.springframework.xd.dirt.plugins;

import java.util.List;

import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.xd.dirt.core.XDRuntimeException;
import org.springframework.xd.module.ModuleType;


/**
 * Thrown when something is wrong with options passed to a module.
 * 
 * @author David Turanski
 */
@SuppressWarnings("serial")
public class ModuleConfigurationException extends XDRuntimeException {

	public ModuleConfigurationException(String message) {
		super(message);
	}

	public ModuleConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Construct an exception following a configuration exception on a particular module.
	 */
	public static ModuleConfigurationException fromBindException(String name, ModuleType type, BindException e) {
		StringBuilder sb = new StringBuilder("Error with option(s) for module ");
		sb.append(name).append(" of type ").append(type.name()).append(":");

		List<FieldError> errors = e.getBindingResult().getFieldErrors();
		for (FieldError error : errors) {
			sb.append("\n    ").append(error.getField()).append(" ").append(error.getDefaultMessage());
		}

		ModuleConfigurationException result = new ModuleConfigurationException(sb.toString(), e);
		return result;
	}
}
