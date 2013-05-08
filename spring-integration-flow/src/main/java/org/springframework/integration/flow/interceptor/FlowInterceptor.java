/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.integration.flow.interceptor;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.interceptor.ChannelInterceptorAdapter;
import org.springframework.integration.flow.FlowConstants;
import org.springframework.integration.support.MessageBuilder;

/**
 * A ChannelInterceptor to set the internal flow output channel name in a header 
 * @see FlowConstants
 * 
 * @author David Turanski
 * @since 3.0
 * 
 */
public class FlowInterceptor extends ChannelInterceptorAdapter {
	private static Log log = LogFactory.getLog(FlowInterceptor.class);

	private final String channelName;

	/**
	 * @param channelName the value of the message header
	 */
	public FlowInterceptor(String channelName) {
		this.channelName = channelName;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		if (log.isDebugEnabled()) {
			log.debug(this + " received a message from port " + channelName + " on channel " + channel);
		}
		
		Map<String, Object> headersToCopy = Collections.singletonMap(FlowConstants.FLOW_OUTPUT_CHANNEL_HEADER,
				(Object) channelName);
		
		return MessageBuilder.fromMessage(message).copyHeaders(headersToCopy).build();
	}
}
