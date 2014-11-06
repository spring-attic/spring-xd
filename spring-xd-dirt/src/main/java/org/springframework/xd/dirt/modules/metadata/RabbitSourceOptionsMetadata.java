/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.modules.metadata;

import org.hibernate.validator.constraints.NotBlank;

import org.springframework.xd.module.options.mixins.MappedRequestHeadersMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ModulePlaceholders;

/**
 * Describes options to the {@code rabbit} source module.
 *
 * @author Eric Bottard
 * @author Gary Russell
 * @author David Liu
 */
@Mixin({ RabbitConnectionMixin.class, MappedRequestHeadersMixin.Amqp.class })
public class RabbitSourceOptionsMetadata {

	private String queues = ModulePlaceholders.XD_STREAM_NAME;

	private String ackMode = "AUTO";

	private boolean transacted = false;

	private int concurrency = 1;

	private boolean requeue = true;

	private int maxConcurrency = 1;

	private int prefetch = 1;

	private int txSize = 1;

	private int initialInterval = 3000;

	private int maxInterval = 30000;

	private int maxAttempts = 3;

	private Long xMessageTtl = 30000L;

	private String dlgQueueName = "dlg";

	private String converterClass = "org.springframework.amqp.support.converter.SimpleMessageConverter";

	@NotBlank
	public String getQueues() {
		return queues;
	}

	@ModuleOption("the queue(s) from which messages will be received")
	public void setQueues(String queues) {
		this.queues = queues;
	}

	@NotBlank
	public String getAckMode() {
		return ackMode.toUpperCase();
	}

	@ModuleOption("the acknowledge mode (AUTO, NONE, MANUAL)")
	public void setAckMode(String ackMode) {
		this.ackMode = ackMode;
	}

	public boolean isTransacted() {
		return transacted;
	}

	@ModuleOption("true if the channel is to be transacted")
	public void setTransacted(boolean transacted) {
		this.transacted = transacted;
	}

	public int getConcurrency() {
		return concurrency;
	}

	@ModuleOption("the minimum number of consumers")
	public void setConcurrency(int concurrency) {
		this.concurrency = concurrency;
	}

	public boolean isRequeue() {
		return requeue;
	}

	@ModuleOption("whether rejected messages will be requeued by default")
	public void setRequeue(boolean requeue) {
		this.requeue = requeue;
	}

	public int getMaxConcurrency() {
		return maxConcurrency;
	}

	@ModuleOption("the maximum number of consumers")
	public void setMaxConcurrency(int maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	public int getPrefetch() {
		return prefetch;
	}

	@ModuleOption("the prefetch size")
	public void setPrefetch(int prefetch) {
		this.prefetch = prefetch;
	}

	public int getTxSize() {
		return txSize;
	}

	@ModuleOption("the number of messages to process before acking")
	public void setTxSize(int txSize) {
		this.txSize = txSize;
	}

	public String getConverterClass() {
		return converterClass;
	}

	@ModuleOption("the class name of the message converter")
	public void setConverterClass(String converterClass) {
		this.converterClass = converterClass;
	}


	public int getInitialInterval() {
		return initialInterval;
	}

	@ModuleOption("initial interval of the backOffPolicy")
	public void setInitialInterval(int initialInterval) {
		this.initialInterval = initialInterval;
	}


	public int getMaxInterval() {
		return maxInterval;
	}

	@ModuleOption("max interval of the backOffPolicy")
	public void setMaxInterval(int maxInterval) {
		this.maxInterval = maxInterval;
	}


	public int getMaxAttempts() {
		return maxAttempts;
	}

	@ModuleOption("max attempts of the retry")
	public void setMaxAttempts(int maxAttempts) {
		this.maxAttempts = maxAttempts;
	}


	public Long getxMessageTtl() {
		return xMessageTtl;
	}

	@ModuleOption("message time to live")
	public void setxMessageTtl(Long xMessageTtl) {
		this.xMessageTtl = xMessageTtl;
	}


	public String getDlgQueueName() {
		return dlgQueueName;
	}

	@ModuleOption("dead message queue name")
	public void setDlgQueueName(String dlgQueueName) {
		this.dlgQueueName = dlgQueueName;
	}


}
