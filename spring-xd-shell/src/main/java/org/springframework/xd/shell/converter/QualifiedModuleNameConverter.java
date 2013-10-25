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

package org.springframework.xd.shell.converter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.MethodTarget;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.xd.rest.client.domain.ModuleDefinitionResource;
import org.springframework.xd.rest.client.domain.RESTModuleType;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.command.ModuleCommands;
import org.springframework.xd.shell.command.ModuleCommands.QualifiedModuleName;


/**
 * Knows how to build and query {@link ModuleCommands.QualifiedModuleName}s.
 * 
 * @author Eric Bottard
 */
@Component
public class QualifiedModuleNameConverter implements Converter<QualifiedModuleName> {

	@Autowired
	private XDShell xdShell;

	@Override
	public boolean supports(Class<?> type, String optionContext) {
		return QualifiedModuleName.class.isAssignableFrom(type);
	}

	@Override
	public QualifiedModuleName convertFromText(String value, Class<?> targetType, String optionContext) {
		int colon = value.indexOf(':');
		QualifiedModuleName result = new QualifiedModuleName(value.substring(colon + 1),
				RESTModuleType.valueOf(value.substring(0, colon)));
		return result;
	}

	@Override
	public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData,
			String optionContext, MethodTarget target) {
		for (ModuleDefinitionResource m : xdShell.getSpringXDOperations().moduleOperations().list(null)) {
			String value = m.getType() + ":" + m.getName();
			completions.add(new Completion(value, m.getName(), pretty(m.getType()), 0));
		}
		return true;

	}

	private String pretty(String type) {
		return StringUtils.capitalize(type) + "s";
	}
}
