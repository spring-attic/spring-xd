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
 * Captures options for the {@code jms} sink module.
 *
 * @author liujiong
 */
public class JmsSinkModuleOptionsMetadata {

	private String provider = "activemq";

	private boolean explicitQosEnabled = false;

	private boolean pubSub = false;

	private String destinationExpression = "'" + XD_STREAM_NAME + "'";

	private boolean sessionTransacted = false;

	private boolean deliveryPersistent = false;

	private int priority;

	private int timeToLive;


	public boolean isExplicitQosEnabled() {
		return explicitQosEnabled;
	}


	public String getDestinationExpression() {
		return destinationExpression;
	}

	private void setExplicitQosEnabled() {
		explicitQosEnabled = true;
	}

	@ModuleOption("a reference to a javax.jms.Destination by bean name")
	public void setDestinationExpression(String destinationExpression) {
		setExplicitQosEnabled();
		this.destinationExpression = destinationExpression;
	}

	public boolean isSessionTransacted() {
		return sessionTransacted;
	}

	@ModuleOption("when true, enables transactions for the message send.")
	public void setSessionTransacted(boolean sessionTransacted) {
		setExplicitQosEnabled();
		this.sessionTransacted = sessionTransacted;
	}


	public boolean isDeliveryPersistent() {
		return deliveryPersistent;
	}

	@ModuleOption("indicating whether the delivery mode should be DeliveryMode.PERSISTENT (true) or DeliveryMode.NON_PERSISTENT (false).")
	public void setDeliveryPersistent(boolean deliveryPersistent) {
		setExplicitQosEnabled();
		this.deliveryPersistent = deliveryPersistent;
	}


	public int getPriority() {
		return priority;
	}

	@ModuleOption("specify the default priority of the message.")
	public void setPriority(int priority) {
		setExplicitQosEnabled();
		this.priority = priority;
	}


	public int getTimeToLive() {
		return timeToLive;
	}

	@ModuleOption("specify the message time to live.")
	public void setTimeToLive(int timeToLive) {
		setExplicitQosEnabled();
		this.timeToLive = timeToLive;
	}

	@NotNull
	public String getProvider() {
		return provider;
	}

	public boolean isPubSub() {
		return pubSub;
	}


	@ModuleOption("the JMS provider")
	public void setProvider(String provider) {
		this.provider = provider;
	}

	@ModuleOption("when true, indicates that the destination is a topic")
	public void setPubSub(boolean pubSub) {
		setExplicitQosEnabled();
		this.pubSub = pubSub;
	}


}
