/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.container.initializer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;

/**
 * An {@link OrderedContextInitializer} to scan resource locations xd.extensions.locations
 * 
 * @author David Turanski
 */
public class PluginContextResourceExtensionsInitializer extends AbstractResourceBeanDefinitionProvider {

	@Value("${xd.extensions.locations:}")
	private String extensionsLocations;

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	protected String getExtensionsLocations() {
		return this.extensionsLocations;
	}
}
