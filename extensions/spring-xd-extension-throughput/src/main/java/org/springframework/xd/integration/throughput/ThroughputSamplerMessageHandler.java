/*
 * Copyright 2013-2014 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.xd.integration.throughput;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * Samples throughput by counting messages over an elapsed time between receiving a
 * {@link org.springframework.messaging.Message} with the given {@code startMessage} and a {@code endMessage} and
 * reports the throughput in the given {@link java.util.concurrent.TimeUnit}.
 * 
 * @author Jon Brisbin
 */
public class ThroughputSamplerMessageHandler implements MessageHandler {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final AtomicLong counter = new AtomicLong();

	private volatile Object startMessage;

	private volatile Object endMessage;

	private volatile TimeUnit sampleUnit;

	private volatile long start;

	private volatile long end;

	private volatile double elapsed;

	private volatile long throughput;

	/**
	 * Create a {@literal ThroughputSamplerMessageHandler} using the {@code String} {@literal START} as the start
	 * message and the {@code String} {@literal END} as the end message and measure throughput in seconds.
	 */
	public ThroughputSamplerMessageHandler() {
		this("START", "END", TimeUnit.SECONDS);
	}

	public ThroughputSamplerMessageHandler(Object startMessage, Object endMessage, TimeUnit sampleUnit) {
		this.startMessage = startMessage;
		this.endMessage = endMessage;
		this.sampleUnit = sampleUnit;
	}

	public ThroughputSamplerMessageHandler(String startMessage, String endMessage, String sampleUnit) {
		this.startMessage = startMessage;
		this.endMessage = endMessage;
		this.sampleUnit = TimeUnit.valueOf(sampleUnit.toUpperCase());
	}

	public Object getStartMessage() {
		return startMessage;
	}

	public void setStartMessage(Object startMessage) {
		this.startMessage = startMessage;
	}

	public Object getEndMessage() {
		return endMessage;
	}

	public void setEndMessage(Object endMessage) {
		this.endMessage = endMessage;
	}

	public TimeUnit getSampleUnit() {
		return sampleUnit;
	}

	public void setSampleUnit(String sampleUnit) {
		setSampleUnit(TimeUnit.valueOf(sampleUnit.toUpperCase()));
	}

	public void setSampleUnit(TimeUnit sampleUnit) {
		Assert.isTrue(sampleUnit == TimeUnit.SECONDS ||
				sampleUnit == TimeUnit.MILLISECONDS ||
				sampleUnit == TimeUnit.NANOSECONDS,
				"Throughput must be reported in SECONDS, MILLISECONDS, or NANOSECONDS");
		this.sampleUnit = sampleUnit;
	}

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		Object payload = message.getPayload();
		if (payload == startMessage || startMessage.equals(payload)) {
			start = System.currentTimeMillis();
		}
		else if (payload == endMessage || endMessage.equals(payload)) {
			end = System.currentTimeMillis();
			elapsed = end - start;
			String unit;
			switch (sampleUnit) {
				case SECONDS:
					throughput = (long) (counter.get() / (elapsed / 1000));
					unit = "s";
					break;
				case NANOSECONDS:
					throughput = (long) (counter.get() / (elapsed * 1000000));
					unit = "ns";
					break;
				default:
					throughput = (long) (counter.get() / elapsed);
					unit = "ms";
			}

			log.info("Throughput sampled for {} items: {}/{} in {}ms elapsed time.",
					counter.get(),
					throughput,
					unit,
					(long) elapsed);

			resetCounter();
		}
		else if (start > 0) {
			counter.incrementAndGet();
		}
	}

	private void resetCounter() {
		counter.set(0);
		start = end = throughput = 0;
		elapsed = 0;
	}

}
