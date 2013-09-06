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

import static org.springframework.xd.module.ModuleType.PROCESSOR;
import static org.springframework.xd.module.ModuleType.SINK;
import static org.springframework.xd.module.ModuleType.SOURCE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.x.channel.registry.ChannelRegistry;
import org.springframework.util.CollectionUtils;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.module.BeanDefinitionAddingPostProcessor;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.Module;
import org.springframework.xd.module.Plugin;

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

	private static final String SPEL_CONTEXT_XML = CONTEXT_CONFIG_ROOT + "spel-context.xml";

	private static final String CHANNEL_REGISTRY = CONTEXT_CONFIG_ROOT + "channel-registry.xml";

	private final static String CONTENT_TYPE_BEAN_NAME = "accepted-content-types";

	private final static Collection<MediaType> DEFAULT_ACCEPTED_CONTENT_TYPES = Collections.singletonList(MediaType.ALL);

	@Override
	public void preProcessModule(Module module) {
		String type = module.getType();
		DeploymentMetadata md = module.getDeploymentMetadata();

		if ((SOURCE.equals(type) || PROCESSOR.equals(type) || SINK.equals(type))) {
			Properties properties = new Properties();
			properties.setProperty("xd.stream.name", md.getGroup());
			properties.setProperty("xd.module.index", String.valueOf(md.getIndex()));
			module.addProperties(properties);
			module.addComponents(new ClassPathResource(SPEL_CONTEXT_XML));
		}
	}

	@Override
	public void postProcessModule(Module module) {
		ChannelRegistry registry = findRegistry(module);
		createInbound(module, registry);
		createOutbound(module, registry);
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

	private void createInbound(Module module, ChannelRegistry registry) {
		DeploymentMetadata md = module.getDeploymentMetadata();
		MessageChannel channel = module.getComponent("input", MessageChannel.class);
		if (channel != null) {
			if (isChannelPubSub(md.getInputChannelName())) {
				registry.createInboundPubSub(md.getInputChannelName(), channel, getAcceptedMediaTypes(module));
			}
			else {
				registry.createInbound(md.getInputChannelName(), channel, getAcceptedMediaTypes(module),
						md.isAliasedInput());
			}
		}
	}

	private void createOutbound(Module module, ChannelRegistry registry) {
		DeploymentMetadata md = module.getDeploymentMetadata();
		MessageChannel channel = module.getComponent("output", MessageChannel.class);
		if (channel != null) {
			if (isChannelPubSub(md.getOutputChannelName())) {
				registry.createOutboundPubSub(md.getOutputChannelName(), channel);
			}
			else {
				registry.createOutbound(md.getOutputChannelName(), channel, md.isAliasedOutput());
			}
			// Create the tap channel now for possible future use (tap:mystream.mymodule)
			if (channel instanceof AbstractMessageChannel) {
				String tapChannelName = getTapChannelName(module);
				DirectChannel tapChannel = new DirectChannel();
				tapChannel.setBeanName(tapChannelName + ".tap.bridge");
				((AbstractMessageChannel) channel).addInterceptor(new WireTap(tapChannel));
				registry.createOutboundPubSub(tapChannelName, tapChannel);
			}
		}
	}

	@Override
	public void beforeShutdown(Module module) {
		ChannelRegistry registry = findRegistry(module);
		if (registry != null) {
			removeInbound(module, registry);
			removeOutbound(module, registry);
		}
	}

	@Override
	public void removeModule(Module module) {
	}

	private void removeInbound(Module module, ChannelRegistry registry) {
		MessageChannel inputChannel = module.getComponent("input", MessageChannel.class);
		if (inputChannel != null) {
			registry.deleteInbound(module.getDeploymentMetadata().getInputChannelName(), inputChannel);
		}
	}

	private void removeOutbound(Module module, ChannelRegistry registry) {
		MessageChannel outputChannel = module.getComponent("output", MessageChannel.class);
		if (outputChannel != null) {
			registry.deleteOutbound(module.getDeploymentMetadata().getOutputChannelName(), outputChannel);
		}
		registry.deleteOutbound(getTapChannelName(module));
	}

	private String getTapChannelName(Module module) {
		return TAP_CHANNEL_PREFIX + module.getDeploymentMetadata().getGroup() + "." + module.getName();
	}

	private boolean isChannelPubSub(String channelName) {
		return channelName != null && channelName.startsWith(TAP_CHANNEL_PREFIX);
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
					acceptedMediaTypes.add(MediaType.parseMediaType((String) acceptedType));
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
				new ClassPathResource(CHANNEL_REGISTRY)));
	}

}
