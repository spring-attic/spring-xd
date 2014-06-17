/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.modules.metadata;

import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_STREAM_NAME;

import javax.validation.constraints.NotNull;

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Captures options for the {@code jms} source module.
 * 
 * @author Eric Bottard 
 */
public class JmsSourceModuleOptionsMetadata {

	private String provider = "activemq";

	private String jmsUrl = "tcp://localhost:61616";

	private String destination = XD_STREAM_NAME;

	private boolean pubSub = false;

	private boolean durableSubscription = false;

	private String subscriptionName = null;

	private String clientId = null;

	@NotNull
	public String getProvider() {
		return provider;
	}

	public String getjmsUUrl() {
		return jmsUrl;
	}

	@NotNull
	public String getDestination() {
		return destination;
	}


	public boolean isPubSub() {
		return pubSub;
	}


	public boolean isDurableSubscription() {
		return durableSubscription;
	}


	public String getSubscriptionName() {
		return subscriptionName;
	}


	public String getClientId() {
		return clientId;
	}

	@ModuleOption("the JMS provider")
	public void setProvider(String provider) {
		this.provider = provider;
	}

	@ModuleOption("the JMS MQ broker URL")
	public void setJmsUrl(String jmsUrl) {
		this.jmsUrl = jmsUrl;
	}

	@ModuleOption("the destination name from which messages will be received")
	public void setDestination(String destination) {
		this.destination = destination;
	}

	@ModuleOption("when true, indicates that the destination is a topic")
	public void setPubSub(boolean pubSub) {
		this.pubSub = pubSub;
	}

	@ModuleOption("when true, indicates the subscription to a topic is durable")
	public void setDurableSubscription(boolean durableSubscription) {
		this.durableSubscription = durableSubscription;
	}

	@ModuleOption("a name that will be assigned to the topic subscription")
	public void setSubscriptionName(String subscriptionName) {
		this.subscriptionName = subscriptionName;
	}

	@ModuleOption("an identifier for the client, to be associated with a durable topic subscription")
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}


}
