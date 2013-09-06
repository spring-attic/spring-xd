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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.util.ConverterUtils;

import org.springframework.yarn.YarnSystemConstants;
import org.springframework.yarn.am.AppmasterService;
import org.springframework.yarn.am.ContainerLauncherInterceptor;
import org.springframework.yarn.am.StaticEventingAppmaster;
import org.springframework.yarn.am.container.AbstractLauncher;

/**
 * Application Master for XD system running on Hadoop.
 * <p>
 * XdAppmaster itself is embedding XD's Admin component which is defined in a context configuration. Its other main
 * purpose is to launch and monitor Yarn managed containers which are used as XD containers.
 * 
 * @author Janne Valkealahti
 * 
 */
public class XdYarnAppmaster extends StaticEventingAppmaster
		implements ContainerLauncherInterceptor {

	private final static Log log = LogFactory.getLog(XdYarnAppmaster.class);

	/**
	 * Shutdowns the XD system managed by this Application Master.
	 */
	public void shutdownXdSystem() {
		notifyCompleted();
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		if (getLauncher() instanceof AbstractLauncher) {
			((AbstractLauncher) getLauncher()).addInterceptor(this);
		}
	}

	@Override
	public ContainerLaunchContext preLaunch(ContainerLaunchContext context) {
		if (log.isDebugEnabled()) {
			log.debug("preLaunch: " + context);
		}

		AppmasterService service = getAppmasterService();
		if (service != null) {
			int port = service.getPort();
			String address = service.getHost();
			Map<String, String> env = new HashMap<String, String>(context.getEnvironment());
			env.put(YarnSystemConstants.AMSERVICE_PORT, Integer.toString(port));
			env.put(YarnSystemConstants.AMSERVICE_HOST, address);
			env.put(YarnSystemConstants.SYARN_CONTAINER_ID, ConverterUtils.toString(context.getContainerId()));
			context.setEnvironment(env);
		}
		return context;
	}

	@Override
	protected void onContainerCompleted(ContainerStatus status) {
		// for now we don't care this because we just
		// let the app run and expected it to be killed.
		// so no need to handle any of this
	}

}
