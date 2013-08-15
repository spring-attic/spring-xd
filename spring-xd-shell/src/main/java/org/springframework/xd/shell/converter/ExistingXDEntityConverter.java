/*
 * Copyright 2002-2013 the original author or authors.
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
import org.springframework.xd.rest.client.SpringXDOperations;
import org.springframework.xd.rest.client.domain.NamedResource;
import org.springframework.xd.shell.XDShell;

/**
 * A no-op converter from String to String, but that helps with completion in names of already existing entities.
 * 
 * @author Eric Bottard
 */
@Component
public class ExistingXDEntityConverter implements Converter<String> {

	@Autowired
	private XDShell xdShell;

	/**
	 * Option Context of arguments that want to benefit from this converter should have this suffix present, with the
	 * appended part representing the kind of entity they're interested in (<i>e.g.</i> {@code existing-stream}).
	 */
	private static final String EXISTING_PREFIX = "existing-";

	@Override
	public boolean supports(Class<?> type, String optionContext) {
		return String.class.isAssignableFrom(type)
				&& (optionContext != null && optionContext.contains(EXISTING_PREFIX));
	}

	@Override
	public String convertFromText(String value, Class<?> targetType, String optionContext) {
		return value;
	}

	@Override
	public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData,
			String optionContext, MethodTarget target) {
		String kind = determineKind(optionContext);
		SpringXDOperations springXDOperations = xdShell.getSpringXDOperations();
		if ("stream".equals(kind)) {
			populate(completions, springXDOperations.streamOperations().list());
		}
		else if ("tap".equals(kind)) {
			populate(completions, springXDOperations.tapOperations().list());
		}
		else if ("job".equals(kind)) {
			populate(completions, springXDOperations.jobOperations().list());
		}
		else if ("trigger".equals(kind)) {
			populate(completions, springXDOperations.triggerOperations().list());
		}
		else if ("counter".equals(kind)) {
			populate(completions, springXDOperations.counterOperations().list());
		}
		else if ("fvc".equals(kind)) {
			populate(completions, springXDOperations.fvcOperations().list());
		}
		else if ("gauge".equals(kind)) {
			populate(completions, springXDOperations.gaugeOperations().list());
		}
		else if ("rich-gauge".equals(kind)) {
			populate(completions, springXDOperations.richGaugeOperations().list());
		}
		else if ("aggregate-counter".equals(kind)) {
			populate(completions, springXDOperations.aggrCounterOperations().list());
		}
		else {
			throw new IllegalArgumentException("Unsupported kind: " + kind);
		}
		return true;
	}

	private void populate(List<Completion> completions, Iterable<? extends NamedResource> resources) {
		for (NamedResource named : resources) {
			completions.add(new Completion(named.getName()));
		}
	}

	private String determineKind(String optionContext) {
		String[] options = optionContext.split(" ");
		for (String option : options) {
			if (option.startsWith(EXISTING_PREFIX)) {
				return option.substring(EXISTING_PREFIX.length());
			}
		}
		throw new IllegalStateException("Could not determine kind: " + optionContext);
	}

}
