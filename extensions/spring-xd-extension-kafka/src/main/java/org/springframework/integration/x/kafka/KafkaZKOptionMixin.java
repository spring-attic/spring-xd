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

package org.springframework.integration.x.kafka;

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Kafka module options mixin for ZK configuration.
 *
 * @author Ilayaperumal Gopinathan
 */
public class KafkaZKOptionMixin {

	private String zkconnect = "localhost:2181";

	private int zksessionTimeout = 6000;

	private int zkconnectionTimeout = 6000;

	private int zksyncTime = 2000;

	public String getZkconnect() {
		return zkconnect;
	}

	@ModuleOption("zookeeper connect string")
	public void setZkconnect(String zkconnect) {
		this.zkconnect = zkconnect;
	}

	public int getZksessionTimeout() {
		return zksessionTimeout;
	}

	@ModuleOption("zookeeper session timeout in milliseconds")
	public void setZksessionTimeout(int zksessionTimeout) {
		this.zksessionTimeout = zksessionTimeout;
	}

	public int getZkconnectionTimeout() {
		return zkconnectionTimeout;
	}

	@ModuleOption("the max time the client waits to connect to ZK in milliseconds")
	public void setZkconnectionTimeout(int zkconnectionTimeout) {
		this.zkconnectionTimeout = zkconnectionTimeout;
	}

	public int getZksyncTime() {
		return zksyncTime;
	}

	@ModuleOption("how far a ZK follower can be behind a ZK leader in milliseconds")
	public void setZksyncTime(int zksyncTime) {
		this.zksyncTime = zksyncTime;
	}
}
