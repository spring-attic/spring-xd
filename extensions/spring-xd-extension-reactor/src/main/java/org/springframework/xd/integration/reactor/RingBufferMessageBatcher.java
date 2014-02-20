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

/**
 * @author Jon Brisbin
 */
public class RingBufferMessageBatcher
		extends MessageProducerSupport
		implements MessageHandler, InitializingBean {

	private final List messagePayloads = new ArrayList();

	private final int        batchSize;
	private final RingBuffer ringBuffer;

	private MessageHeaders messageHeaders;
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

	@SuppressWarnings("unchecked")
	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		// get sequence id
		long seqId = ringBuffer.next();
		// if this is the first message of the batch
		if(null == messageHeaders) {
			// save the start id
			sequenceStart = seqId;
			// pull the headers from the first message
			messageHeaders = message.getHeaders();
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
			sequenceStart = -1;
		}
	}


}
