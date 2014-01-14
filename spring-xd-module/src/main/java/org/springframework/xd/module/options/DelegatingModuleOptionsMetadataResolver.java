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

package org.springframework.xd.module.options;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.xd.module.ModuleDefinition;

/**
 * A {@link ModuleOptionsMetadataResolver} that will invoke several other {@link ModuleOptionsMetadataResolver}
 * instances in turn and merge their results (flattening them).
 * 
 * @author Eric Bottard
 */
public class DelegatingModuleOptionsMetadataResolver implements ModuleOptionsMetadataResolver {

	private final List<ModuleOptionsMetadataResolver> delegates;

	public DelegatingModuleOptionsMetadataResolver(List<ModuleOptionsMetadataResolver> delegates) {
		Assert.notEmpty(delegates, "at least one delegate resolver is required");
		this.delegates = delegates;
	}

	@Override
	public ModuleOptionsMetadata resolve(ModuleDefinition moduleDefinition) {
		List<ModuleOptionsMetadata> moms = new ArrayList<ModuleOptionsMetadata>(delegates.size());
		for (ModuleOptionsMetadataResolver delegate : delegates) {
			ModuleOptionsMetadata resolved = delegate.resolve(moduleDefinition);
			if (resolved != null) {
				moms.add(resolved);
			}
		}
		return new FlattenedCompositeModuleOptionsMetadata(moms);
	}

}
