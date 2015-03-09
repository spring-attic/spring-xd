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

import static org.springframework.integration.x.kafka.AutoOffsetResetStrategy.smallest;

import javax.validation.constraints.NotNull;

import kafka.serializer.Decoder;
import kafka.serializer.DefaultDecoder;

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Module options for Kafka consumer configuration.
 *
 * @author Ilayaperumal Gopinathan
 * @author Marius Bogoevici
 */
public class KafkaConsumerOptionsMixin {

	private int socketTimeout = 30000;

	private int socketBufferBytes = 2 * 1024 * 1024;

	private int fetchMaxBytes = 1024 * 1024;

	private int fetchMinBytes = 1;

	private int fetchMaxWait = 100;

	private AutoOffsetResetStrategy autoOffsetReset = smallest;

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

	@NotNull(message = "AutoOffset Reset must either be 'smallest' or 'largest'")
	public AutoOffsetResetStrategy getAutoOffsetReset() {
		return autoOffsetReset;
	}

	@ModuleOption("strategy to reset the offset when there is no initial offset in ZK or if an offset is out of range")
	public void setAutoOffsetReset(AutoOffsetResetStrategy autoOffsetReset) {
		this.autoOffsetReset = autoOffsetReset;
	}

	public long getAutoOffsetResetValue() {
		return autoOffsetReset.code();
	}

}
