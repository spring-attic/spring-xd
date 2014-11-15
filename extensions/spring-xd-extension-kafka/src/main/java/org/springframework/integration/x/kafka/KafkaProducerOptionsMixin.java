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
 * Module options for Kafka Producer configuration.
 *
 * @author Ilayaperumal Gopinathan
 */
public class KafkaProducerOptionsMixin {

	private int requestRequiredAck = 0;

	private int requestTimeout = 10000;

	private String producerType = "sync";

	private String compressionCodec = "none";

	private String compressedTopics = "";

	private int maxSendRetries = 3;

	private int retryBackoff = 100;

	private int topicMetadataRefreshInterval = 600 * 1000;

	private int maxBufferTime = 5000;

	private int maxBufferMsgs = 10000;

	private int enqueueTimeout = -1;

	private int batchCount = 200;

	private int socketBufferSize = 100 * 1024;

	public int getRequestRequiredAck() {
		return requestRequiredAck;
	}

	@ModuleOption("producer request acknowledgement mode")
	public void setRequestRequiredAck(int requestRequiredAck) {
		this.requestRequiredAck = requestRequiredAck;
	}

	@AssertTrue(message = "requestRequiredAck can have values 0, 1 or -1")
	public boolean isRequestRequiredAckValid() {
		return (this.requestRequiredAck >= -1) && (this.requestRequiredAck <= 1);
	}

	public int getRequestTimeout() {
		return requestTimeout;
	}

	@ModuleOption("timeout in milliseconds after waiting for request required ack")
	public void setRequestTimeout(int requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	public String getProducerType() {
		return producerType;
	}

	@ModuleOption("producer type")
	public void setProducerType(String producerType) {
		this.producerType = producerType;
	}

	@AssertTrue(message = "produer type can either be sync or async")
	public boolean isProducerTypeValid() {
		return (this.producerType.equals("sync") || this.producerType.equals("async"));
	}

	public String getCompressionCodec() {
		return compressionCodec;
	}

	@ModuleOption("compression codec to use")
	public void setCompressionCodec(String compressionCodec) {
		this.compressionCodec = compressionCodec;
	}

	@AssertTrue(message = "compression codec can be 'none', 'gzip' or 'snappy'")
	public boolean isValidCompressionCodec() {
		return this.compressionCodec.equals("none") || this.compressionCodec.equals("gzip")
				|| this.compressionCodec.equals("snappy");
	}

	public String getCompressedTopics() {
		return compressedTopics;
	}

	@ModuleOption("comma separated list of topics to apply the compression codec")
	public void setCompressedTopics(String compressedTopics) {
		this.compressedTopics = compressedTopics;
	}

	public int getMaxSendRetries() {
		return maxSendRetries;
	}

	@ModuleOption("number of attempts to automatically retry a failed send request")
	public void setMaxSendRetries(int maxSendRetries) {
		this.maxSendRetries = maxSendRetries;
	}

	public int getRetryBackoff() {
		return retryBackoff;
	}

	@ModuleOption("amount of time the producer waits before refreshing the metadata")
	public void setRetryBackoff(int retryBackoff) {
		this.retryBackoff = retryBackoff;
	}


	public int getTopicMetadataRefreshInterval() {
		return topicMetadataRefreshInterval;
	}

	@ModuleOption("topic metadata refresh interval")
	public void setTopicMetadataRefreshInterval(int topicMetadataRefreshInterval) {
		this.topicMetadataRefreshInterval = topicMetadataRefreshInterval;
	}

	public int getMaxBufferTime() {
		return maxBufferTime;
	}

	@ModuleOption("maximum time in milliseconds to buffer data when using async mode")
	public void setMaxBufferTime(int maxBufferTime) {
		this.maxBufferTime = maxBufferTime;
	}

	public int getMaxBufferMsgs() {
		return maxBufferMsgs;
	}

	@ModuleOption("the maximum number of unsent messages that can be queued up the async producer")
	public void setMaxBufferMsgs(int maxBufferMsgs) {
		this.maxBufferMsgs = maxBufferMsgs;
	}

	public int getEnqueueTimeout() {
		return enqueueTimeout;
	}

	@ModuleOption("the amount of time to block before dropping messages when running in async mode")
	public void setEnqueueTimeout(int enqueueTimeout) {
		this.enqueueTimeout = enqueueTimeout;
	}

	public int getBatchCount() {
		return batchCount;
	}

	@ModuleOption("the number of messages to send in one batch when using async mode")
	public void setBatchCount(int batchCount) {
		this.batchCount = batchCount;
	}

	public int getSocketBufferSize() {
		return socketBufferSize;
	}

	@ModuleOption("socket write buffer size")
	public void setSocketBufferSize(int socketBufferSize) {
		this.socketBufferSize = socketBufferSize;
	}
}
