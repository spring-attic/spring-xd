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

package org.springframework.xd.integration.reactor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.ProducerType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * Implementation of a {@link org.springframework.messaging.MessageHandler} that batches payloads until the {@code
 * RingBuffer} is full and then flushes them into the delegate {@link org.springframework.messaging.MessageHandler} in
 * a new message that is the aggregation of all the message payloads in the batch.
 * <p>
 * This message batcher is safe to use from multiple threads. However, if its known in advance that a single writer
 * will
 * be using this batcher, the more performant {@code ProducerType.SINGLE} can be used.
 * </p>
 *
 * @author Jon Brisbin
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class RingBufferMessageBatcher extends MessageProducerSupport implements MessageHandler,
                                                                                InitializingBean {

	private static final AtomicLongFieldUpdater<RingBufferMessageBatcher> SEQ_START =
			AtomicLongFieldUpdater.newUpdater(RingBufferMessageBatcher.class, "sequenceStart");

	private final List messagePayloads = new ArrayList();

	private final int        batchSize;
	private final RingBuffer ringBuffer;

	private volatile MessageHeaders messageHeaders;
	private volatile long sequenceStart = -1;

	public RingBufferMessageBatcher(int batchSize) {
		this(batchSize, ProducerType.MULTI, new BlockingWaitStrategy());
	}

	public RingBufferMessageBatcher(int batchSize,
	                                ProducerType producerType,
	                                WaitStrategy waitStrategy) {
		this.batchSize = batchSize;
		this.ringBuffer = RingBuffer.create(
				producerType,
				new EventFactory() {
					@Override
					public Object newInstance() {
						return new Object();
					}
				},
				batchSize,
				waitStrategy
		);
	}

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		// get sequence id
		long seqId = ringBuffer.next();
		// if this is the first message of the batch
		if(null == messageHeaders) {
			// save the start id
			if(SEQ_START.compareAndSet(this, -1, sequenceStart)) {
				// pull the headers from the first message
				messageHeaders = message.getHeaders();
			}
		}
		// add all payloads
		messagePayloads.add(message.getPayload());

		// ring buffer is now full
		if(seqId % batchSize == 0) {
			Message<?> msg = new GenericMessage<Object>(new ArrayList<Object>(messagePayloads), messageHeaders);
			// send batched message
			sendMessage(msg);
			// clear payloads and headers
			messagePayloads.clear();
			messageHeaders = null;
			// reset ring buffer
			long start = sequenceStart;
			ringBuffer.publish(start, seqId);
			SEQ_START.compareAndSet(this, sequenceStart, -1);
		}
	}


}
