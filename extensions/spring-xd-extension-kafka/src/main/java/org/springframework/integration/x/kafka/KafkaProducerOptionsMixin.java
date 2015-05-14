/*
 * Copyright 2014-2015 the original author or authors.
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
 * @author Marius Bogoevici
 */
public class KafkaProducerOptionsMixin {

	private int requestRequiredAck = 0;

	private int bufferMemory = 33554432;

	private String compressionCodec = "none";

	private int maxSendRetries = 3;

	private int batchBytes = 16384;

	private int maxRequestSize = 1048576;

	private int maxBufferTime = 0;

	private int receiveBufferBytes = 32768;

	private int sendBufferBytes = 131072;

	private int ackTimeoutOnServer = 30000;

	private boolean blockOnBufferFull = true;

	private int topicMetadataRefreshInterval = 300000;

	private int topicMetadataFetchTimeout = 60000;

	private long reconnectBackoff = 10;

	private long retryBackoff = 100;

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

	public int getBufferMemory() {
		return bufferMemory;
	}
	@ModuleOption("the total bytes of memory the producer can use to buffer records waiting to be sent to the server")
	public void setBufferMemory(int bufferMemory) {
		this.bufferMemory = bufferMemory;
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

	public int getMaxSendRetries() {
		return maxSendRetries;
	}

	@ModuleOption("number of attempts to automatically retry a failed send request")
	public void setMaxSendRetries(int maxSendRetries) {
		this.maxSendRetries = maxSendRetries;
	}


	public int getBatchBytes() {
		return batchBytes;
	}

	@ModuleOption("batch size in bytes, per partition")
	public void setBatchBytes(int batchBytes) {
		this.batchBytes = batchBytes;
	}
	public int getMaxRequestSize() {
		return maxRequestSize;
	}

	@ModuleOption("the maximum size of a request")
	public void setMaxRequestSize(int maxRequestSize) {
		this.maxRequestSize = maxRequestSize;
	}

	public int getMaxBufferTime() {
		return maxBufferTime;
	}

	@ModuleOption("the amount of time, in ms that the producer will wait before sending a batch to the server")
	public void setMaxBufferTime(int maxBufferTime) {
		this.maxBufferTime = maxBufferTime;
	}

	public int getReceiveBufferBytes() {
		return receiveBufferBytes;
	}

	@ModuleOption("the size of the TCP receive buffer to use when reading data")
	public void setReceiveBufferBytes(int receiveBufferBytes) {
		this.receiveBufferBytes = receiveBufferBytes;
	}

	public int getSendBufferBytes() {
		return sendBufferBytes;
	}

	@ModuleOption("the size of the TCP send buffer to use when sending data")
	public void setSendBufferBytes(int sendBufferBytes) {
		this.sendBufferBytes = sendBufferBytes;
	}

	public int getAckTimeoutOnServer() {
		return ackTimeoutOnServer;
	}

	@ModuleOption("the maximum amount of time the server will wait for acknowledgments from followers to meet the " +
			"acknowledgment requirements the producer has specified with the acks configuration")
	public void setAckTimeoutOnServer(int ackTimeoutOnServer) {
		this.ackTimeoutOnServer = ackTimeoutOnServer;
	}

	public boolean isBlockOnBufferFull() {
		return blockOnBufferFull;
	}

	@ModuleOption("whether to block or not when the memory buffer is full")
	public void setBlockOnBufferFull(boolean blockOnBufferFull) {
		this.blockOnBufferFull = blockOnBufferFull;
	}

	public int getTopicMetadataRefreshInterval() {
		return topicMetadataRefreshInterval;
	}

	@ModuleOption("the period of time in milliseconds after which a refresh of metadata is forced")
	public void setTopicMetadataRefreshInterval(int topicMetadataRefreshInterval) {
		this.topicMetadataRefreshInterval = topicMetadataRefreshInterval;
	}

	public int getTopicMetadataFetchTimeout() {
		return topicMetadataFetchTimeout;
	}

	@ModuleOption("the maximum amount of time to block waiting for the metadata fetch to succeed")
	public void setTopicMetadataFetchTimeout(int topicMetadataFetchTimeout) {
		this.topicMetadataFetchTimeout = topicMetadataFetchTimeout;
	}

	public long getReconnectBackoff() {
		return reconnectBackoff;
	}

	@ModuleOption("the amount of time to wait before attempting to reconnect to a given host when a connection fails")
	public void setReconnectBackoff(long reconnectBackoff) {
		this.reconnectBackoff = reconnectBackoff;
	}

	public long getRetryBackoff() {
		return retryBackoff;
	}

	@ModuleOption("the amount of time to wait before attempting to retry a failed produce request to a given" +
			" topic partition")
	public void setRetryBackoff(long retryBackoff) {
		this.retryBackoff = retryBackoff;
	}
}
