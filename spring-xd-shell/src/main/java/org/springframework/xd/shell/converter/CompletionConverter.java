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

package org.springframework.xd.shell.converter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.MethodTarget;
import org.springframework.stereotype.Component;
import org.springframework.xd.rest.client.CompletionOperations;
import org.springframework.xd.rest.client.domain.CompletionKind;
import org.springframework.xd.shell.XDShell;


/**
 * A converter that provides DSL completion wherever parts of stream definitions may appear.
 * 
 * @author Eric Bottard
 */
@Component
public class CompletionConverter implements Converter<String> {

	@Autowired
	private XDShell xdShell;

	/**
	 * To appear in the optionContext. Triggers the use of this converter and specifies which kind of completion is
	 * expected.
	 */
	private static final String COMPLETION_CONTEXT_PREFIX = "completion-";

	@Override
	public boolean supports(Class<?> type, String optionContext) {
		return type == String.class && optionContext.contains(COMPLETION_CONTEXT_PREFIX);
	}

	@Override
	public String convertFromText(String value, Class<?> targetType, String optionContext) {
		return value;
	}

	@Override
	public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData,
			String optionContext, MethodTarget target) {
		String start = (existingData.startsWith("'") || existingData.startsWith("\"")) ? existingData.substring(1)
				: existingData;

		CompletionKind kind = determineKind(optionContext);
		try {
			List<String> candidates = completionOperations().completions(kind,
					start);
			for (String candidate : candidates) {
				completions.add(new Completion(candidate));
			}
			return false;
		}
		// Protect from exception in non-command code
		catch (Exception e) {
			return false;
		}
	}

	private CompletionOperations completionOperations() {
		return xdShell.getSpringXDOperations().completionOperations();
	}

	private CompletionKind determineKind(String optionContext) {
		String[] options = optionContext.split(" ");
		for (String option : options) {
			if (option.startsWith(COMPLETION_CONTEXT_PREFIX)) {
				return CompletionKind.valueOf(option.substring(COMPLETION_CONTEXT_PREFIX.length()));
			}
		}
		throw new IllegalStateException("Could not determine kind: " + optionContext);
	}


}
