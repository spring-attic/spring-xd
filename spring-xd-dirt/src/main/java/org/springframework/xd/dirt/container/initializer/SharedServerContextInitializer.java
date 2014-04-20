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
import org.springframework.xd.dirt.util.ConfigLocations;

/**
 * An {@link OrderedContextInitializer} to load XD SharedServerContext resources. Doing it here ensures these beans are
 * loaded prior to any Shared Server context extensions.
 *
 * @author David Turanski
 */
public class SharedServerContextInitializer extends AbstractResourceBeanDefinitionProvider {

	@Value("${XD_ANALYTICS}")
	private String XD_ANALYTICS;

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	protected String[] getLocations() {
		return new String[] {
			ConfigLocations.XD_CONFIG_ROOT + "bus/*.xml",
			ConfigLocations.XD_CONFIG_ROOT + "internal/repositories.xml",
			ConfigLocations.XD_CONFIG_ROOT + "analytics/" + XD_ANALYTICS + "-analytics.xml" };
	}

	@Override
	protected String getExtensionsLocations() {
		return null;
	}

	@Override
	public TargetContext getTargetContext() {
		return TargetContext.SHARED_SERVER_CONTEXT;
	}
}
