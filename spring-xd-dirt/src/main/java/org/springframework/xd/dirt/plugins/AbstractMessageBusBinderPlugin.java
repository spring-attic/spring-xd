/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.plugins;

import org.springframework.integration.channel.ChannelInterceptorAware;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.Plugin;


/**
 * Abstract {@link Plugin} that has common implementation methods to bind/unbind {@link Module}'s message producers and
 * consumers to/from {@link MessageBus}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author David Turanski
 * @author Jennifer Hickey
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 */
public abstract class AbstractMessageBusBinderPlugin extends AbstractPlugin {

	protected static final String MODULE_INPUT_CHANNEL = "input";

	protected static final String MODULE_OUTPUT_CHANNEL = "output";

	protected static final String TAP_CHANNEL_PREFIX = "tap:";

	protected static final String TOPIC_CHANNEL_PREFIX = "topic:";

	protected static final String JOB_CHANNEL_PREFIX = "job:";

	protected final MessageBus messageBus;

	public AbstractMessageBusBinderPlugin(MessageBus messageBus) {
		Assert.notNull(messageBus, "messageBus cannot be null.");
		this.messageBus = messageBus;
	}

	/**
	 * Bind input/output channel of the module's message consumer/producers to {@link MessageBus}'s message
	 * source/target entities.
	 *
	 * @param module the module whose consumer and producers to bind to the {@link MessageBus}.
	 */
	protected final void bindConsumerAndProducers(Module module) {
		MessageChannel inputChannel = module.getComponent(MODULE_INPUT_CHANNEL, MessageChannel.class);
		if (inputChannel != null) {
			bindMessageConsumer(inputChannel, getInputChannelName(module));
		}
		MessageChannel outputChannel = module.getComponent(MODULE_OUTPUT_CHANNEL, MessageChannel.class);
		if (outputChannel != null) {
			bindMessageProducer(outputChannel, getOutputChannelName(module));
			createAndBindTapChannel(module, outputChannel);
		}
	}

	protected abstract String getInputChannelName(Module module);

	protected abstract String getOutputChannelName(Module module);

	private void bindMessageConsumer(MessageChannel inputChannel, String inputChannelName) {
		if (isChannelPubSub(inputChannelName)) {
			messageBus.bindPubSubConsumer(inputChannelName, inputChannel);
		}
		else {
			messageBus.bindConsumer(inputChannelName, inputChannel);
		}
	}

	private void bindMessageProducer(MessageChannel outputChannel, String outputChannelName) {
		if (isChannelPubSub(outputChannelName)) {
			messageBus.bindPubSubProducer(outputChannelName, outputChannel);
		}
		else {
			messageBus.bindProducer(outputChannelName, outputChannel);
		}
	}

	/**
	 * Creates a wiretap on the output channel of the {@link Module} and binds the tap channel to {@link MessageBus}'s
	 * message target.
	 *
	 * @param module the module whose output channel to tap
	 * @param outputChannel the channel to tap
	 */
	private void createAndBindTapChannel(Module module, MessageChannel outputChannel) {
		if (outputChannel instanceof ChannelInterceptorAware) {
			String tapChannelName = buildTapChannelName(module);
			MessageChannel tapChannel = tapOutputChannel(tapChannelName, (ChannelInterceptorAware) outputChannel);
			messageBus.bindPubSubProducer(tapChannelName, tapChannel);
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("output channel is not interceptor aware. Tap will not be created.");
			}
		}
	}

	private MessageChannel tapOutputChannel(String tapChannelName, ChannelInterceptorAware outputChannel) {
		DirectChannel tapChannel = new DirectChannel();
		tapChannel.setBeanName(tapChannelName + ".tap.bridge");
		outputChannel.addInterceptor(new WireTap(tapChannel));
		return tapChannel;
	}

	/**
	 * Unbind input/output channel of the module's message consumer/producers from {@link MessageBus}'s message
	 * source/target entities.
	 *
	 * @param module the module whose consumer and producers to unbind from the {@link MessageBus}.
	 */
	protected final void unbindConsumerAndProducers(Module module) {
		MessageChannel inputChannel = module.getComponent(MODULE_INPUT_CHANNEL, MessageChannel.class);
		if (inputChannel != null) {
			messageBus.unbindConsumer(getInputChannelName(module), inputChannel);
		}
		MessageChannel outputChannel = module.getComponent(MODULE_OUTPUT_CHANNEL, MessageChannel.class);
		if (outputChannel != null) {
			messageBus.unbindProducer(getOutputChannelName(module), outputChannel);
			unbindTapChannel(module);
		}
	}

	private void unbindTapChannel(Module module) {
		// Should this be unbindProducer() as there won't be multiple producers on the tap channel.
		messageBus.unbindProducers(buildTapChannelName(module));
	}

	private String buildTapChannelName(Module module) {
		Assert.isTrue(module.getType() != ModuleType.job, "Job module type not supported.");
		DeploymentMetadata dm = module.getDeploymentMetadata();
		// for Stream return channel name with indexed elements
		return String.format("%s%s.%s.%s", TAP_CHANNEL_PREFIX, dm.getGroup(), module.getName(), dm.getIndex());
	}

	private boolean isChannelPubSub(String channelName) {
		Assert.isTrue(StringUtils.hasText(channelName), "Channel name should not be empty/null.");
		// Check if the channelName starts with tap: or topic:
		return (channelName.startsWith(TAP_CHANNEL_PREFIX) || channelName.startsWith(TOPIC_CHANNEL_PREFIX));
	}

	@Override
	public int getOrder() {
		return 0;
	}

}
