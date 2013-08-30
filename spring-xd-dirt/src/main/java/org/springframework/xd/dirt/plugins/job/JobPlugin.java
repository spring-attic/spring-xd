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

package org.springframework.xd.dirt.plugins.job;

import static org.springframework.xd.module.ModuleType.JOB;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.MediaType;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.x.channel.registry.ChannelRegistry;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.module.AbstractPlugin;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.Module;

/**
 * Plugin to enable the registration of jobs in a central registry.
 * 
 * @author Michael Minella
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Glenn Renfro
 * @since 1.0
 * 
 */
public class JobPlugin extends AbstractPlugin {

	private final Log logger = LogFactory.getLog(getClass());

	private static final String CONTEXT_CONFIG_ROOT = XDContainer.XD_CONFIG_ROOT
			+ "plugins/job/";

	private static final String REGISTRAR = CONTEXT_CONFIG_ROOT + "job-module-beans.xml";

	private static final String DATE_FORMAT = "dateFormat";

	private static final String NUMBER_FORMAT = "numberFormat";

	private static final String MAKE_UNIQUE = "makeUnique";

	public static final String JOB_BEAN_ID = "job";

	public static final String JOB_NAME_DELIMITER = ".";

	private static final String NOTIFICATION_CHANNEL_SUFFIX = "-notifications";

	private static final String JOB_CHANNEL_PREFIX = "job:";

	private final static Collection<MediaType> DEFAULT_ACCEPTED_CONTENT_TYPES = Collections.singletonList(MediaType.ALL);

	@Override
	public void configureProperties(Module module) {
		final Properties properties = new Properties();
		properties.setProperty("xd.stream.name", module.getDeploymentMetadata().getGroup());

		if (!module.getProperties().contains(DATE_FORMAT)) {
			properties.setProperty(DATE_FORMAT, "");
		}
		if (!module.getProperties().contains(NUMBER_FORMAT)) {
			properties.setProperty(NUMBER_FORMAT, "");
		}
		if (!module.getProperties().contains(MAKE_UNIQUE)) {
			properties.setProperty(MAKE_UNIQUE, String.valueOf(Boolean.TRUE));
		}

		if (logger.isInfoEnabled()) {
			logger.info("Configuring module with the following properties: " + properties.toString());
		}

		module.addProperties(properties);

	}

	@Override
	public void postProcessModule(Module module) {
		ChannelRegistry registry = findRegistry(module);
		DeploymentMetadata md = module.getDeploymentMetadata();
		if (registry != null) {
			MessageChannel inputChannel = module.getComponent("input", MessageChannel.class);
			if (inputChannel != null) {
				registry.createInbound(JOB_CHANNEL_PREFIX + md.getGroup(), inputChannel,
						DEFAULT_ACCEPTED_CONTENT_TYPES,
						true);
			}
			MessageChannel notificationsChannel = module.getComponent("serializedNotifications", MessageChannel.class);
			if (notificationsChannel != null) {
				registry.createOutbound(md.getGroup() + NOTIFICATION_CHANNEL_SUFFIX, notificationsChannel, true);
			}
		}
	}

	private ChannelRegistry findRegistry(Module module) {
		ChannelRegistry registry = null;
		try {
			registry = module.getComponent(ChannelRegistry.class);
		}
		catch (Exception e) {
			logger.error("No registry in context, cannot wire channels");
		}
		return registry;
	}

	@Override
	public void removeModule(Module module) {
		ChannelRegistry registry = findRegistry(module);
		if (registry != null) {
			registry.deleteInbound(JOB_CHANNEL_PREFIX + module.getDeploymentMetadata().getGroup());
			registry.deleteOutbound(module.getDeploymentMetadata().getGroup() + NOTIFICATION_CHANNEL_SUFFIX);
		}
	}

	@Override
	public List<String> componentPathsSelector(Module module) {
		List<String> result = new ArrayList<String>();
		if (!JOB.equals(module.getType())) {
			return result;
		}
		result.add(REGISTRAR);
		return result;
	}

}
