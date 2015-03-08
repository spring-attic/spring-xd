/*
 * Copyright 2013-2015 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.utils.ThreadUtils;

import org.springframework.integration.channel.ChannelInterceptorAware;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.integration.bus.BusUtils;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.integration.bus.MessageBus.Capability;
import org.springframework.xd.dirt.integration.bus.XdHeaders;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnectionListener;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.Plugin;
import org.springframework.xd.module.options.spi.ModulePlaceholders;

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

	protected static final String JOB_CHANNEL_PREFIX = "job:";

	protected final MessageBus messageBus;

	/**
	 * Cache of children under the taps path.
	 */
	private volatile PathChildrenCache taps;

	/**
	 * A {@link PathChildrenCacheListener} implementation that monitors tap additions and removals.
	 */
	private final TapListener tapListener = new TapListener();

	/**
	 * Map of channels that can be tapped. The keys are the tap channel names (e.g. tap:stream:ticktock.time.0),
	 * and the values are the output channels from modules where the actual WireTap interceptors would be added.
	 */
	private final Map<String, MessageChannel> tappableChannels = new HashMap<String, MessageChannel>();

	public AbstractMessageBusBinderPlugin(MessageBus messageBus) {
		this(messageBus, null);
	}

	public AbstractMessageBusBinderPlugin(MessageBus messageBus, ZooKeeperConnection zkConnection) {
		Assert.notNull(messageBus, "MessageBus must not be null.");
		this.messageBus = messageBus;
		if (zkConnection != null) {
			if (zkConnection.isConnected()) {
				startTapListener(zkConnection.getClient());
			}
			zkConnection.addListener(new TapLifecycleConnectionListener());
		}
	}

	private void startTapListener(CuratorFramework client) {
		String tapPath = Paths.build(Paths.TAPS);
		Paths.ensurePath(client, tapPath);
		taps = new PathChildrenCache(client, tapPath, true,
				ThreadUtils.newThreadFactory("TapsPathChildrenCache"));
		taps.getListenable().addListener(tapListener);
		try {
			taps.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e, "failed to start TapListener");
		}
	}

	/**
	 * Bind input/output channel of the module's message consumer/producers to {@link MessageBus}'s message
	 * source/target entities. The producer must be bound first so that messages can immediately
	 * start flowing from the consumer.
	 *
	 * @param module the module whose consumer and producers to bind to the {@link MessageBus}.
	 */
	protected final void bindConsumerAndProducers(final Module module) {
		boolean trackHistory = module.getDeploymentProperties() != null
				? module.getDeploymentProperties().getTrackHistory()
				: false;
		Properties[] properties = extractConsumerProducerProperties(module);
		Map<String, Object> historyProperties = null;
		if (trackHistory) {
			historyProperties = extractHistoryProperties(module);
			addHistoryTag(module, historyProperties);
		}
		MessageChannel outputChannel = module.getComponent(MODULE_OUTPUT_CHANNEL, MessageChannel.class);
		if (outputChannel != null) {
			bindMessageProducer(outputChannel, getOutputChannelName(module), properties[1]);
			String tapChannelName = buildTapChannelName(module);
			tappableChannels.put(tapChannelName, outputChannel);
			if (isTapActive(tapChannelName)) {
				createAndBindTapChannel(tapChannelName, outputChannel);
			}
			if (trackHistory) {
				track(module, outputChannel, historyProperties);
			}
		}
		MessageChannel inputChannel = module.getComponent(MODULE_INPUT_CHANNEL, MessageChannel.class);
		if (inputChannel != null) {
			bindMessageConsumer(inputChannel, getInputChannelName(module), module.getDescriptor().getGroup(),
					properties[0]);
			if (trackHistory && module.getType().equals(ModuleType.sink)) {
				track(module, inputChannel, historyProperties);
			}
		}
	}

	private void addHistoryTag(Module module, Map<String, Object> historyProperties) {
		String historyTag = module.getDescriptor().getModuleLabel();
		if (module.getDescriptor().getSinkChannelName() != null) {
			historyTag += ">" + module.getDescriptor().getSinkChannelName();
		}
		if (module.getDescriptor().getSourceChannelName() != null) {
			historyTag = module.getDescriptor().getSourceChannelName() + ">" + historyTag;
		}
		historyProperties.put("module", historyTag);
	}

	private void track(final Module module, MessageChannel channel, final Map<String, Object> historyProps) {
		final MessageBuilderFactory messageBuilderFactory = module.getComponent(
				IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME,
				MessageBuilderFactory.class) == null
				? new DefaultMessageBuilderFactory()
				: module.getComponent(
						IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME,
						MessageBuilderFactory.class);
		if (channel instanceof ChannelInterceptorAware) {
			((ChannelInterceptorAware) channel).addInterceptor(new ChannelInterceptorAdapter() {

				@Override
				public Message<?> preSend(Message<?> message, MessageChannel channel) {
					@SuppressWarnings("unchecked")
					Collection<Map<String, Object>> history =
							(Collection<Map<String, Object>>) message.getHeaders().get(XdHeaders.XD_HISTORY);
					if (history == null) {
						history = new ArrayList<Map<String, Object>>(1);
					}
					else {
						history = new ArrayList<Map<String, Object>>(history);
					}
					Map<String, Object> map = new LinkedHashMap<String, Object>();
					map.putAll(historyProps);
					map.put("thread", Thread.currentThread().getName());
					history.add(map);
					Message<?> out = messageBuilderFactory
							.fromMessage(message)
							.setHeader(XdHeaders.XD_HISTORY, history)
							.build();
					map.put("timestamp", out.getHeaders().getTimestamp());
					return out;
				}
			});
		}
	}

	protected final Properties[] extractConsumerProducerProperties(Module module) {
		Properties consumerProperties = new Properties();
		Properties producerProperties = new Properties();
		String consumerKeyPrefix = "consumer.";
		String producerKeyPrefix = "producer.";
		if (module.getDeploymentProperties() != null) {
			for (Map.Entry<String, String> entry : module.getDeploymentProperties().entrySet()) {
				if (entry.getKey().startsWith(consumerKeyPrefix)) {
					consumerProperties.put(entry.getKey().substring(consumerKeyPrefix.length()), entry.getValue());
				}
				else if (entry.getKey().startsWith(producerKeyPrefix)) {
					producerProperties.put(entry.getKey().substring(producerKeyPrefix.length()), entry.getValue());
				}
			}
		}
		return new Properties[] { consumerProperties, producerProperties };
	}

	protected final Map<String, Object> extractHistoryProperties(Module module) {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		if (module.getProperties() != null) {
			for (Map.Entry<Object, Object> entry : module.getProperties().entrySet()) {
				if (entry.getKey() instanceof String) {
					String key = (String) entry.getKey();
					if (key.startsWith(ModulePlaceholders.XD_CONTAINER_KEY_PREFIX)) {
						key = key.substring(ModulePlaceholders.XD_CONTAINER_KEY_PREFIX.length());
						if (key.equals("id")) {
							key = "container.id";
						}
						properties.put(key, entry.getValue());
					}
					else if (key.equals(ModulePlaceholders.XD_STREAM_NAME_KEY)) {
						properties.put(key.substring(3), entry.getValue());
					}
				}
			}
		}
		return properties;
	}

	@Override
	public void beforeShutdown(Module module) {
		unbindConsumer(module);
	}

	@Override
	public void removeModule(Module module) {
		super.removeModule(module);
		unbindProducers(module);
	}

	protected abstract String getInputChannelName(Module module);

	protected abstract String getOutputChannelName(Module module);

	protected abstract String buildTapChannelName(Module module);

	private void bindMessageConsumer(MessageChannel inputChannel, String inputChannelName,
			String group, Properties consumerProperties) {
		if (BusUtils.isChannelPubSub(inputChannelName)) {
			String channelToBind = inputChannelName;
			if (this.messageBus.isCapable(Capability.DURABLE_PUBSUB)) {
				channelToBind = BusUtils.addGroupToPubSub(group, inputChannelName);
			}
			messageBus.bindPubSubConsumer(channelToBind, inputChannel, consumerProperties);
		}
		else {
			messageBus.bindConsumer(inputChannelName, inputChannel, consumerProperties);
		}
	}

	private void bindMessageProducer(MessageChannel outputChannel, String outputChannelName,
			Properties producerProperties) {
		if (BusUtils.isChannelPubSub(outputChannelName)) {
			messageBus.bindPubSubProducer(outputChannelName, outputChannel, producerProperties);
		}
		else {
			messageBus.bindProducer(outputChannelName, outputChannel, producerProperties);
		}
	}

	/**
	 * Creates a wiretap on the output channel of the {@link Module} and binds the tap channel to {@link MessageBus}'s
	 * message target.
	 *
	 * @param tapChannelName the name of the tap channel
	 * @param outputChannel the channel to tap
	 */
	private void createAndBindTapChannel(String tapChannelName, MessageChannel outputChannel) {
		logger.info("creating and binding tap channel for {}", tapChannelName);
		if (outputChannel instanceof ChannelInterceptorAware) {
			DirectChannel tapChannel = new DirectChannel();
			tapChannel.setBeanName(tapChannelName + ".tap.bridge");
			messageBus.bindPubSubProducer(tapChannelName, tapChannel, null); // TODO tap producer props
			tapOutputChannel(tapChannel, (ChannelInterceptorAware) outputChannel);
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("output channel is not interceptor aware. Tap will not be created.");
			}
		}
	}

	private MessageChannel tapOutputChannel(MessageChannel tapChannel, ChannelInterceptorAware outputChannel) {
		outputChannel.addInterceptor(new WireTap(tapChannel));
		return tapChannel;
	}

	/**
	 * Unbind the input channel of the module from the {@link MessageBus}
	 * (stop sending new messages to the module so it can be stopped).
	 *
	 * @param module the module for which the consumer is to be unbound from the {@link MessageBus}.
	 */
	protected void unbindConsumer(Module module) {
		MessageChannel inputChannel = module.getComponent(MODULE_INPUT_CHANNEL, MessageChannel.class);
		if (inputChannel != null) {
			String channelToUnbind = getInputChannelName(module);
			if (this.messageBus.isCapable(Capability.DURABLE_PUBSUB)) {
				channelToUnbind = BusUtils.addGroupToPubSub(module.getDescriptor().getGroup(), channelToUnbind);
			}
			messageBus.unbindConsumer(channelToUnbind, inputChannel);
			if (logger.isDebugEnabled()) {
				logger.debug("Unbound consumer for " + module.toString());
			}
		}
	}

	/**
	 * Unbind the output channel of the module (and tap if present) from the {@link MessageBus}
	 * (after it has been stopped).
	 *
	 * @param module the module for which producers are to be unbound from the {@link MessageBus}.
	 */
	protected void unbindProducers(Module module) {
		MessageChannel outputChannel = module.getComponent(MODULE_OUTPUT_CHANNEL, MessageChannel.class);
		if (outputChannel != null) {
			messageBus.unbindProducer(getOutputChannelName(module), outputChannel);
			String tapChannelName = buildTapChannelName(module);
			unbindTapChannel(tapChannelName);
			tappableChannels.remove(tapChannelName);
			if (logger.isDebugEnabled()) {
				logger.debug("Unbound producer(s) for " + module.toString());
			}
		}
	}

	private void unbindTapChannel(String tapChannelName) {
		// Should this be unbindProducer() as there won't be multiple producers on the tap channel.
		MessageChannel tappedChannel = tappableChannels.get(tapChannelName);
		if (tappedChannel instanceof ChannelInterceptorAware) {
			ChannelInterceptorAware interceptorAware = ((ChannelInterceptorAware) tappedChannel);
			List<ChannelInterceptor> interceptors = new ArrayList<ChannelInterceptor>();
			for (ChannelInterceptor interceptor : interceptorAware.getChannelInterceptors()) {
				if (interceptor instanceof WireTap) {
					((WireTap) interceptor).stop();
				}
				else {
					interceptors.add(interceptor);
				}
			}
			interceptorAware.setInterceptors(interceptors);
			messageBus.unbindProducers(tapChannelName);
		}
	}

	@Override
	public int getOrder() {
		return 0;
	}


	/**
	 * Event handler for tap additions.
	 *
	 * @param data module data
	 */
	private void onTapAdded(ChildData data) {
		String tapChannelName = buildTapChannelNameFromPath(data.getPath());
		MessageChannel outputChannel = tappableChannels.get(tapChannelName);
		if (outputChannel != null) {
			createAndBindTapChannel(tapChannelName, outputChannel);
		}
	}

	/**
	 * Event handler for tap removals.
	 *
	 * @param data module data
	 */
	private void onTapRemoved(ChildData data) {
		unbindTapChannel(buildTapChannelNameFromPath(data.getPath()));
	}

	/**
	 * Checks whether the provided tap channel name has one or more active subscribers.
	 *
	 * @param tapChannelName the tap channel to check
	 *
	 * @return {@code true} if the tap does have one or more active subscribers
	 */
	private boolean isTapActive(String tapChannelName) {
		Assert.state(taps != null, "tap cache not started");
		List<ChildData> currentTaps = taps.getCurrentData();
		for (ChildData data : currentTaps) {
			// example path: /taps/stream:ticktock.time.0
			if (buildTapChannelNameFromPath(data.getPath()).equals(tapChannelName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Generates the name of a tap channel given a ZooKeeper tap path.
	 *
	 * @param path the ZooKeeper path under {@link Paths#TAPS}.
	 *
	 * @return the tap channel name
	 */
	private String buildTapChannelNameFromPath(String path) {
		return BusUtils.TAP_CHANNEL_PREFIX + Paths.stripPath(path);
	}


	/**
	 * Listener for tap additions and removals under {@link Paths#TAPS}.
	 */
	class TapListener implements PathChildrenCacheListener {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
			ZooKeeperUtils.logCacheEvent(logger, event);
			switch (event.getType()) {
				case INITIALIZED:
					break;
				case CHILD_ADDED:
					onTapAdded(event.getData());
					break;
				case CHILD_REMOVED:
					onTapRemoved(event.getData());
					break;
				default:
					break;
			}
		}
	}


	/**
	 * A {@link ZooKeeperConnectionListener} that manages the lifecycle of the taps cache listener.
	 */
	class TapLifecycleConnectionListener implements ZooKeeperConnectionListener {

		@Override
		public void onDisconnect(CuratorFramework client) {
			taps.getListenable().removeListener(tapListener);
			try {
				taps.close();
			}
			catch (Exception e) {
				throw ZooKeeperUtils.wrapThrowable(e);
			}
		}

		@Override
		public void onSuspend(CuratorFramework client) {
		}

		@Override
		public void onConnect(CuratorFramework client) {
			startTapListener(client);
		}

		@Override
		public void onResume(CuratorFramework client) {
		}
	}

}
