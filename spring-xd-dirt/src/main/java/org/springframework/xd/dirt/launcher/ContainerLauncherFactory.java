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

package org.springframework.xd.dirt.launcher;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.dirt.server.options.ContainerOptions;
import org.springframework.xd.dirt.server.options.OptionUtils;


/**
 * Class that encapsulates the creation and configuration of a ContainerLauncher
 * 
 * @author Mark Pollack
 */
public class ContainerLauncherFactory {

	private static final String LAUNCHER_CONFIG_LOCATION = XDContainer.XD_INTERNAL_CONFIG_ROOT + "launcher.xml";

	/**
	 * Creates a new instance of ContainerLauncher.
	 * 
	 * @param options The options that select transport, analytics, and other infrastructure options.
	 * @param parentContext an optional parent context to set on the XDContainer's ApplicationContext.
	 * @return a new ContainerLauncher instance
	 */

	public ContainerLauncher createContainerLauncher(ContainerOptions options, ApplicationContext parentContext) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext();
		context.setConfigLocation(LAUNCHER_CONFIG_LOCATION);


		if (parentContext == null) {
			parentContext = createParentContext(options);
		}

		context.setParent(parentContext);
		context.refresh();

		context.registerShutdownHook();

		ContainerLauncher launcher = context.getBean(ContainerLauncher.class);
		return launcher;
	}

	private static ApplicationContext createParentContext(ContainerOptions options) {
		XmlWebApplicationContext parentContext = new XmlWebApplicationContext();
		parentContext.setConfigLocation("classpath:" + XDContainer.XD_INTERNAL_CONFIG_ROOT + "xd-global-beans.xml");
		OptionUtils.configureRuntime(options, parentContext.getEnvironment());
		parentContext.refresh();
		return parentContext;
	}
}
