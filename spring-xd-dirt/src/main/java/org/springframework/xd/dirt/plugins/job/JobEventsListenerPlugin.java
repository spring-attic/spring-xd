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

package org.springframework.xd.dirt.plugins.job;

import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.integration.bus.BusUtils;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.plugins.AbstractJobPlugin;
import org.springframework.xd.dirt.plugins.job.support.listener.XDJobListenerConstants;
import org.springframework.xd.module.core.Module;

/**
 * Plugin to enable the registration of out of the box job listeners.
 *
 * @author Ilayaperumal Gopinathan
 * @author Gary Russell
 * @since 1.0
 */
public class JobEventsListenerPlugin extends AbstractJobPlugin implements XDJobListenerConstants {

	private static final String JOB_TAP_CHANNEL_PREFIX = BusUtils.TAP_CHANNEL_PREFIX + JOB_CHANNEL_PREFIX;

	public JobEventsListenerPlugin(MessageBus messageBus) {
		super(messageBus);
	}

	@Override
	public void postProcessModule(Module module) {
		boolean disableListeners = true;
		Map<String, String> eventChannels = getEventListenerChannels(module.getDescriptor().getGroup());
		for (Map.Entry<String, String> entry : eventChannels.entrySet()) {
			MessageChannel eventChannel = module.getComponent(entry.getKey(), SubscribableChannel.class);
			if (eventChannel != null) {
				messageBus.bindPubSubProducer(entry.getValue(), eventChannel, null);
				disableListeners = false;
			}
		}
		// Bind aggregatedEvents channel if at least one of the event listeners channels is already bound.
		if (!disableListeners) {
			bindAggregatedEventsChannel(module);
		}
	}

	/**
	 * @param jobName the job name.
	 * @return the map containing the entries for the channels used by the job listeners with bean name of the channel
	 *         as the key and channel name as the value.
	 */
	public static Map<String, String> getEventListenerChannels(String jobName) {
		Map<String, String> eventListenerChannels = new HashMap<String, String>();
		Assert.notNull(jobName, "Job name should not be null");
		eventListenerChannels.put(XD_JOB_EXECUTION_EVENTS_CHANNEL,
				getEventListenerChannelName(jobName, JOB_EXECUTION_EVENTS_SUFFIX));
		eventListenerChannels.put(XD_STEP_EXECUTION_EVENTS_CHANNEL,
				getEventListenerChannelName(jobName, STEP_EXECUTION_EVENTS_SUFFIX));
		eventListenerChannels.put(XD_CHUNK_EVENTS_CHANNEL, getEventListenerChannelName(jobName, CHUNK_EVENTS_SUFFIX));
		eventListenerChannels.put(XD_ITEM_EVENTS_CHANNEL, getEventListenerChannelName(jobName, ITEM_EVENTS_SUFFIX));
		eventListenerChannels.put(XD_SKIP_EVENTS_CHANNEL, getEventListenerChannelName(jobName, SKIP_EVENTS_SUFFIX));
		return eventListenerChannels;
	}


	private static String getEventListenerChannelName(String jobName, String channelNameSuffix) {
		return String.format("%s%s.%s", JOB_TAP_CHANNEL_PREFIX, jobName, channelNameSuffix);
	}

	/**
	 * @param jobName the job name.
	 * @return the aggregated event channel name.
	 */
	public static String getEventListenerChannelName(String jobName) {
		return String.format("%s%s", JOB_TAP_CHANNEL_PREFIX, jobName);
	}

	private void bindAggregatedEventsChannel(Module module) {
		String jobName = module.getDescriptor().getGroup();
		MessageChannel aggEventsChannel = module.getComponent(XD_AGGREGATED_EVENTS_CHANNEL, SubscribableChannel.class);
		Assert.notNull(aggEventsChannel,
				"The pub/sub aggregatedEvents channel should be available in the module context.");
		messageBus.bindPubSubProducer(getEventListenerChannelName(jobName), aggEventsChannel, null);
	}

	@Override
	public void removeModule(Module module) {
		Map<String, String> eventListenerChannels = getEventListenerChannels(module.getDescriptor().getGroup());
		for (Map.Entry<String, String> channelEntry : eventListenerChannels.entrySet()) {
			messageBus.unbindProducers(channelEntry.getValue());
		}
		// unbind aggregatedEvents channel
		messageBus.unbindProducers(getEventListenerChannelName(module.getDescriptor().getGroup()));
	}
}
