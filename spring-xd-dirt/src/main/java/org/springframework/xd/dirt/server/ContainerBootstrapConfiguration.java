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

package org.springframework.xd.dirt.server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.xd.dirt.container.initializer.SharedServerContextInitializer;
import org.springframework.xd.dirt.container.initializer.OrderedContextInitializer;
import org.springframework.xd.dirt.container.initializer.PluginContextComponentScanningExtensionsInitializer;
import org.springframework.xd.dirt.container.initializer.PluginContextResourceExtensionsInitializer;
import org.springframework.xd.dirt.container.initializer.PluginsInitializer;
import org.springframework.xd.dirt.container.initializer.SharedServerContextComponentScanningExtensionsInitializer;
import org.springframework.xd.dirt.container.initializer.SharedServerContextResourceExtensionsInitializer;


/**
 * Bootstrap Configuration for the {@link ContainerBootstrapContext }
 *
 * @author David Turanski
 */
@Configuration
class ContainerBootstrapConfiguration {

	@Bean
	OrderedContextInitializer pluginsComponentExtensionsInitializer() {
		return new PluginContextComponentScanningExtensionsInitializer();
	}

	@Bean
	OrderedContextInitializer pluginsResourceextensionsInitializer() {
		return new PluginContextResourceExtensionsInitializer();
	}

	@Bean
	OrderedContextInitializer sharedServerComponentExtensionsInitializer() {
		return new SharedServerContextComponentScanningExtensionsInitializer();
	}

	@Bean
	OrderedContextInitializer sharedServerResourceExtensionsInitializer() {
		return new SharedServerContextResourceExtensionsInitializer();
	}


	@Bean
	OrderedContextInitializer pluginsInitializer() {
		return new PluginsInitializer();
	}

	@Bean
	OrderedContextInitializer messageBusInitializer() {
		return new SharedServerContextInitializer();
	}

}
