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

package org.springframework.xd.dirt.plugins.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.Advised;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.CollectionUtils;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.Plugin;
import org.springframework.xd.module.support.BeanDefinitionAddingPostProcessor;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author David Turanski
 * @author Jennifer Hickey
 * @author Glenn Renfro
 */
public class StreamPlugin implements Plugin {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private static final String CONTEXT_CONFIG_ROOT = XDContainer.XD_CONFIG_ROOT + "plugins/stream/";

	private static final String TAP_CHANNEL_PREFIX = "tap:";

	private static final String MESSAGE_BUS = CONTEXT_CONFIG_ROOT + "message-bus.xml";

	private final static String CONTENT_TYPE_BEAN_NAME = "accepted-content-types";

	private final static Collection<MediaType> DEFAULT_ACCEPTED_CONTENT_TYPES = Collections.singletonList(MediaType.ALL);

	private static final String TOPIC_CHANNEL_PREFIX = "topic:";

	@Override
	public void preProcessModule(Module module) {
		DeploymentMetadata md = module.getDeploymentMetadata();
		Properties properties = new Properties();
		properties.setProperty("xd.stream.name", md.getGroup());
		properties.setProperty("xd.module.index", String.valueOf(md.getIndex()));
		module.addProperties(properties);
	}

	@Override
	public void postProcessModule(Module module) {
		MessageBus bus = findMessageBus(module);
		bindConsumer(module, bus);
		bindProducers(module, bus);
	}

	@Override
	public boolean supports(Module module) {
		ModuleType moduleType = module.getType();
		return (moduleType == ModuleType.source || moduleType == ModuleType.processor || moduleType == ModuleType.sink);
	}

	protected final MessageBus findMessageBus(Module module) {
		MessageBus messageBus = null;
		try {
			messageBus = module.getComponent(MessageBus.class);
		}
		catch (Exception e) {
			logger.error("No MessageBus in context, cannot wire/unwire channels: " + e.getMessage());
		}
		return messageBus;
	}

	private void bindConsumer(Module module, MessageBus bus) {
		DeploymentMetadata md = module.getDeploymentMetadata();
		MessageChannel channel = module.getComponent("input", MessageChannel.class);
		if (channel != null) {
			if (isChannelPubSub(md.getInputChannelName())) {
				bus.bindPubSubConsumer(md.getInputChannelName(), channel, getAcceptedMediaTypes(module));
			}
			else {
				bus.bindConsumer(md.getInputChannelName(), channel, getAcceptedMediaTypes(module),
						md.isAliasedInput());
			}
		}
	}

	private void bindProducers(Module module, MessageBus bus) {
		DeploymentMetadata md = module.getDeploymentMetadata();
		MessageChannel channel = module.getComponent("output", MessageChannel.class);
		if (channel != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("binding output channel [" + md.getOutputChannelName() + "] for " + module);
			}
			if (isChannelPubSub(md.getOutputChannelName())) {
				bus.bindPubSubProducer(md.getOutputChannelName(), channel);
			}
			else {
				bus.bindProducer(md.getOutputChannelName(), channel, md.isAliasedOutput());
			}

			// TODO remove this once addInterceptor is an interface method in SI
			Object rawChannel = extractTarget(channel);

			// Create the tap channel now for possible future use (tap:mystream.mymodule)
			if (rawChannel instanceof AbstractMessageChannel) {
				String tapChannelName = getTapChannelName(module);
				DirectChannel tapChannel = new DirectChannel();
				tapChannel.setBeanName(tapChannelName + ".tap.bridge");
				((AbstractMessageChannel) rawChannel).addInterceptor(new WireTap(tapChannel));
				bus.bindPubSubProducer(tapChannelName, tapChannel);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("output channel is not an AbstractMessageChannel. Tap will not be created.");
				}
			}
		}
	}

	@Override
	public void beforeShutdown(Module module) {
		MessageBus bus = findMessageBus(module);
		if (bus != null) {
			unbindConsumer(module, bus);
			unbindProducers(module, bus);
		}
	}

	@Override
	public void removeModule(Module module) {
	}

	private void unbindConsumer(Module module, MessageBus bus) {
		MessageChannel inputChannel = module.getComponent("input", MessageChannel.class);
		if (inputChannel != null) {
			bus.unbindConsumer(module.getDeploymentMetadata().getInputChannelName(), inputChannel);
		}
	}

	private void unbindProducers(Module module, MessageBus bus) {
		MessageChannel outputChannel = module.getComponent("output", MessageChannel.class);
		if (outputChannel != null) {
			bus.unbindProducer(module.getDeploymentMetadata().getOutputChannelName(), outputChannel);
		}
		bus.unbindProducers(getTapChannelName(module));
	}

	private String getTapChannelName(Module module) {
		return TAP_CHANNEL_PREFIX + module.getDeploymentMetadata().getGroup() + "." + module.getName();
	}

	private boolean isChannelPubSub(String channelName) {
		return channelName != null
				&& (channelName.startsWith(TAP_CHANNEL_PREFIX) || channelName.startsWith(TOPIC_CHANNEL_PREFIX));
	}

	private Collection<MediaType> getAcceptedMediaTypes(Module module) {
		Collection<?> acceptedTypes = module.getComponent(CONTENT_TYPE_BEAN_NAME, Collection.class);

		if (CollectionUtils.isEmpty(acceptedTypes)) {
			return DEFAULT_ACCEPTED_CONTENT_TYPES;
		}
		else {
			Collection<MediaType> acceptedMediaTypes = new ArrayList<MediaType>(acceptedTypes.size());
			for (Object acceptedType : acceptedTypes) {
				if (acceptedType instanceof String) {
					acceptedMediaTypes.add(MediaType.valueOf((String) acceptedType));
				}
				else if (acceptedType instanceof MediaType) {
					acceptedMediaTypes.add((MediaType) acceptedType);
				}
				else {
					throw new IllegalArgumentException("Unrecognized MediaType :" + acceptedType);
				}
			}
			return Collections.unmodifiableCollection(acceptedMediaTypes);
		}
	}

	@Override
	public void preProcessSharedContext(ConfigurableApplicationContext context) {
		context.addBeanFactoryPostProcessor(new BeanDefinitionAddingPostProcessor(context.getEnvironment(),
				new ClassPathResource(MESSAGE_BUS)));
	}

	// TODO please get me out of this class, preferably by deleting when SI has addInterceptor in an interface
	private Object extractTarget(Object bean) {
		if (!(bean instanceof Advised)) {
			return bean;
		}
		Advised advised = (Advised) bean;
		if (advised.getTargetSource() == null) {
			return null;
		}
		try {
			return extractTarget(advised.getTargetSource().getTarget());
		}
		catch (Exception e) {
			logger.error("Could not extract target from output channel. Tap will not be created.", e);
			return null;
		}
	}
}
