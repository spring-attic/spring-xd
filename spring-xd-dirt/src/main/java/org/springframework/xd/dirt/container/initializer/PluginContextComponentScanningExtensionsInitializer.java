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
 * An {@link OrderedContextInitializer} to scan for annotation configured beans in xd.extensions.basepackages
 * 
 * @author David Turanski
 */
public class PluginContextComponentScanningExtensionsInitializer extends
		AbstractComponentScanningBeanDefinitionProvider {


	@Value("${xd.extensions.basepackages:}")
	private String extensionsBasePackages;

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	protected String getExtensionsBasePackages() {
		return this.extensionsBasePackages;
	}
}
