/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.module.info;

import java.util.ArrayList;
import java.util.List;

import org.springframework.xd.module.ModuleDefinition;

/**
 * A composite resolver that will try delegates until one returns a non {@code null} result.
 *
 * @author Eric Bottard
 */
public class DelegatingModuleInformationResolver implements ModuleInformationResolver{

	private List<ModuleInformationResolver> delegates = new ArrayList<>();

	@Override
	public ModuleInformation resolve(ModuleDefinition definition) {
		for (ModuleInformationResolver resolver : delegates) {
			ModuleInformation result = resolver.resolve(definition);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	public void setDelegates(List<ModuleInformationResolver> delegates) {
		this.delegates = delegates;
	}
}
