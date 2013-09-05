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

package org.springframework.xd.yarn;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.yarn.container.CommandLineContainerRunner;

/**
 * Custom command line runner for Spring XD Yarn Container.
 * <p>
 * This is needed to setup parent application context for context containing xd container launcher bean.
 * 
 * @author Janne Valkealahti
 * 
 */
public class XdYarnContainerRunner extends CommandLineContainerRunner {

	@Override
	protected ConfigurableApplicationContext getApplicationContext(String configLocation) {

		// create the xd global context
		XmlWebApplicationContext parentContext = new XmlWebApplicationContext();
		parentContext.setConfigLocation("classpath:" + XDContainer.XD_INTERNAL_CONFIG_ROOT + "xd-global-beans.xml");
		parentContext.refresh();

		// ask context from a super and hook previously
		// created parent with it
		ConfigurableApplicationContext context = super.getApplicationContext(configLocation);
		context.setParent(parentContext);
		return context;
	}

	public static void main(String[] args) {
		new XdYarnContainerRunner().doMain(args);
	}

}
