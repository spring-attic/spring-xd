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

import javax.validation.constraints.AssertTrue;

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Module options for Kafka consumer configuration.
 *
 * @author Ilayaperumal Gopinathan
 */
public class KafkaConsumerOptionsMixin {

	private int socketTimeout = 30000;

	private int socketBufferBytes = 64 * 1024;

	private int fetchMaxBytes = 300 * 1024;

	private int fetchMinBytes = 1;

	private int fetchMaxWait = 100;

	private String autoOffsetReset = "smallest";

	private int consumerTimeout = 5000;

	public int getSocketTimeout() {
		return socketTimeout;
	}

	@ModuleOption("sock timeout for network requests in milliseconds")
	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public int getSocketBufferBytes() {
		return socketBufferBytes;
	}

	@ModuleOption("socket receive buffer for network requests")
	public void setSocketBufferBytes(int socketBufferBytes) {
		this.socketBufferBytes = socketBufferBytes;
	}

	public int getFetchMaxBytes() {
		return fetchMaxBytes;
	}

	@ModuleOption("max messages to attempt to fetch for each topic-partition in each fetch request")
	public void setFetchMaxBytes(int fetchMaxBytes) {
		this.fetchMaxBytes = fetchMaxBytes;
	}

	public int getFetchMinBytes() {
		return fetchMinBytes;
	}

	@ModuleOption("the minimum amount of data the server should return for a fetch request")
	public void setFetchMinBytes(int fetchMinBytes) {
		this.fetchMinBytes = fetchMinBytes;
	}

	public int getFetchMaxWait() {
		return fetchMaxWait;
	}

	@ModuleOption("max wait time before answering the fetch request")
	public void setFetchMaxWait(int fetchMaxWait) {
		this.fetchMaxWait = fetchMaxWait;
	}

	public String getAutoOffsetReset() {
		return autoOffsetReset;
	}

	@ModuleOption("strategy to reset the offset when there is no initial offset in ZK or if an offset is out of range")
	public void setAutoOffsetReset(String autoOffsetReset) {
		this.autoOffsetReset = autoOffsetReset;
	}

	@AssertTrue(message = "AutoOffset Reset can either be 'smallest' or 'largest'")
	public boolean isValidAutoOffsetReset() {
		return (this.autoOffsetReset.equals("smallest") || this.autoOffsetReset.equals("largest"));
	}
}
