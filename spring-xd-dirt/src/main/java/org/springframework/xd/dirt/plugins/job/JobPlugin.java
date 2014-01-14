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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.AbstractPlugin;
import org.springframework.xd.module.core.Module;

/**
 * Plugin to enable the registration of jobs in a central registry.
 * 
 * @author Michael Minella
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 * @since 1.0
 */
public class JobPlugin extends AbstractPlugin {

	private final Log logger = LogFactory.getLog(getClass());

	private static final String CONTEXT_CONFIG_ROOT = XDContainer.XD_CONFIG_ROOT
			+ "plugins/job/";

	private static final String REGISTRAR = CONTEXT_CONFIG_ROOT + "job-module-beans.xml";

	public static final String JOB_BEAN_ID = "job";

	public static final String JOB_NAME_DELIMITER = ".";

	public static final String JOB_PARAMETERS_KEY = "jobParameters";

	private static final String NOTIFICATION_CHANNEL_SUFFIX = "-notifications";

	private static final String JOB_CHANNEL_PREFIX = "job:";

	private static final String JOB_LAUNCH_REQUEST_CHANNEL = "input";

	private static final String JOB_NOTIFICATIONS_CHANNEL = "notifications";

	private static final String JOB_PARTIONER_REQUEST_CHANNEL = "stepExecutionRequests.output";

	private static final String JOB_PARTIONER_REPLY_CHANNEL = "stepExecutionReplies.input";

	private static final String JOB_STEP_EXECUTION_REQUEST_CHANNEL = "stepExecutionRequests.input";

	private static final String JOB_STEP_EXECUTION_REPLY_CHANNEL = "stepExecutionReplies.output";


	private final static Collection<MediaType> DEFAULT_ACCEPTED_CONTENT_TYPES = Collections.singletonList(MediaType.ALL);

	@Override
	public void configureProperties(Module module) {
		final Properties properties = new Properties();
		properties.setProperty("xd.stream.name", module.getDeploymentMetadata().getGroup());
		module.addProperties(properties);
	}

	@Override
	public void postProcessModule(Module module) {
		MessageBus bus = findMessageBus(module);
		DeploymentMetadata md = module.getDeploymentMetadata();
		if (bus != null) {
			MessageChannel inputChannel = module.getComponent(JOB_LAUNCH_REQUEST_CHANNEL, MessageChannel.class);
			if (inputChannel != null) {
				bus.bindConsumer(JOB_CHANNEL_PREFIX + md.getGroup(), inputChannel,
						DEFAULT_ACCEPTED_CONTENT_TYPES,
						true);
			}
			MessageChannel notificationsChannel = module.getComponent(JOB_NOTIFICATIONS_CHANNEL, MessageChannel.class);
			if (notificationsChannel != null) {
				bus.bindProducer(JOB_CHANNEL_PREFIX + md.getGroup() + NOTIFICATION_CHANNEL_SUFFIX,
						notificationsChannel, true);
			}

			if (module.getComponent(JOB_PARTIONER_REQUEST_CHANNEL, MessageChannel.class) != null) {
				this.processPartitionedJob(module, md, bus);
			}
		}
	}

	private void processPartitionedJob(Module module, DeploymentMetadata md, MessageBus bus) {
		if (logger.isDebugEnabled()) {
			logger.debug("binding job partitioning channels for " + module);
		}
		MessageChannel partitionsOut = module.getComponent(JOB_PARTIONER_REQUEST_CHANNEL, MessageChannel.class);
		Assert.notNull(partitionsOut, "Partitioned jobs must have a " + JOB_PARTIONER_REQUEST_CHANNEL);
		MessageChannel partitionsIn = module.getComponent(JOB_PARTIONER_REPLY_CHANNEL, MessageChannel.class);
		Assert.notNull(partitionsIn, "Partitioned jobs must have a " + JOB_PARTIONER_REPLY_CHANNEL);
		String name = md.getGroup() + "." + md.getIndex();
		bus.bindRequestor(name, partitionsOut, partitionsIn);

		MessageChannel stepExecutionsIn = module.getComponent(JOB_STEP_EXECUTION_REQUEST_CHANNEL, MessageChannel.class);
		Assert.notNull(stepExecutionsIn, "Partitioned jobs must have a " + JOB_STEP_EXECUTION_REQUEST_CHANNEL);
		MessageChannel stepExecutionResultsOut = module.getComponent(JOB_STEP_EXECUTION_REPLY_CHANNEL,
				MessageChannel.class);
		Assert.notNull(stepExecutionResultsOut, "Partitioned jobs must have a " + JOB_STEP_EXECUTION_REPLY_CHANNEL);
		bus.bindReplier(name, stepExecutionsIn, stepExecutionResultsOut);
	}

	private void unbindPartitionedJob(Module module, MessageBus bus) {
		if (logger.isDebugEnabled()) {
			logger.debug("unbinding job partitioning channels for " + module);
		}
		DeploymentMetadata md = module.getDeploymentMetadata();
		MessageChannel partitionsOut = module.getComponent(JOB_PARTIONER_REQUEST_CHANNEL, MessageChannel.class);
		String name = md.getGroup() + "." + md.getIndex();
		if (partitionsOut != null) {
			bus.unbindProducer(name, partitionsOut);
		}
		MessageChannel partitionsIn = module.getComponent(JOB_PARTIONER_REPLY_CHANNEL, MessageChannel.class);
		if (partitionsIn != null) {
			bus.unbindConsumer(name, partitionsIn);
		}
		MessageChannel stepExcutionsIn = module.getComponent(JOB_STEP_EXECUTION_REQUEST_CHANNEL, MessageChannel.class);
		if (stepExcutionsIn != null) {
			bus.unbindConsumer(name, stepExcutionsIn);
		}
		MessageChannel stepExecutionResultsOut = module.getComponent(JOB_STEP_EXECUTION_REPLY_CHANNEL,
				MessageChannel.class);
		if (stepExecutionResultsOut != null) {
			bus.unbindProducer(name, stepExecutionResultsOut);
		}
	}

	@Override
	public boolean supports(Module module) {
		return (module.getType() == ModuleType.job);
	}

	private MessageBus findMessageBus(Module module) {
		MessageBus messageBus = null;
		try {
			messageBus = module.getComponent(MessageBus.class);
		}
		catch (Exception e) {
			logger.error("No MessageBus in context, cannot wire/unwire channels: " + e.getMessage());
		}
		return messageBus;
	}

	@Override
	public void beforeShutdown(Module module) {
	}

	@Override
	public void removeModule(Module module) {
		MessageBus bus = findMessageBus(module);
		if (bus != null) {
			bus.unbindConsumers(JOB_CHANNEL_PREFIX + module.getDeploymentMetadata().getGroup());
			bus.unbindProducers(module.getDeploymentMetadata().getGroup() + NOTIFICATION_CHANNEL_SUFFIX);
			if (module.getComponent(JOB_PARTIONER_REQUEST_CHANNEL, MessageChannel.class) != null) {
				this.unbindPartitionedJob(module, bus);
			}
		}
	}

	@Override
	public List<String> componentPathsSelector(Module module) {
		List<String> result = new ArrayList<String>();
		result.add(REGISTRAR);
		return result;
	}

	public void launch(Module module, Map<String, String> parameters) {
		MessageChannel inputChannel = module.getComponent(JOB_LAUNCH_REQUEST_CHANNEL, MessageChannel.class);
		String payloadJSON =
				(parameters != null && parameters.get(JOB_PARAMETERS_KEY) != null) ? parameters.get(JOB_PARAMETERS_KEY)
						: "";
		Message<?> message = MessageBuilder.withPayload(payloadJSON).build();
		inputChannel.send(message);
	}

}
