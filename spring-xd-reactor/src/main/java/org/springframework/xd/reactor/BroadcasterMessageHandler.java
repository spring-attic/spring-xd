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
package org.springframework.xd.reactor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import reactor.Environment;
import reactor.core.processor.RingBufferProcessor;
import reactor.rx.Stream;
import reactor.rx.Streams;
import reactor.rx.action.support.DefaultSubscriber;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.springframework.util.ReflectionUtils.*;

/**
 * Adapts the item at a time delivery of a {@link org.springframework.messaging.MessageHandler}
 * by delegating processing to a {@link Stream}
 * <p/>
 * The outputStream of the processor is used to create a message and send it to the output channel. If the
 * input channel and output channel are connected to the MessageBus, then data delivered to the input stream via
 * a call to onNext is invoked on the dispatcher thread of the message bus and sending a message to the output
 * channel will involve IO operations on the message bus.
 * <p/>
 * The implementation uses a {@link reactor.core.processor.RingBufferProcessor} with asynchronous dispatch.
 * This has the advantage that the state of the Stream can be shared across all the incoming dispatcher threads that
 * are invoking onNext. It has the disadvantage that processing and sending to the output channel will execute serially
 * on one of the dispatcher threads.
 * <p/>
 * The use of this handler makes for a very natural first experience when processing data. For example given
 * the stream <code></code>http | reactor-processor | log</code> where the <code>reactor-processor</code> does does a
 * <code>buffer(5)</code> and then produces a single value. Sending 10 messages to the http source will
 * result in 2 messages in the log, no matter how many dispatcher threads are used.
 * <p/>
 * You can modify what thread the outputStream subscriber, which does the send to the output channel,
 * will use by explicitly calling <code>dispatchOn</code> or other switch (http://projectreactor.io/docs/reference/#streams-multithreading)
 * before returning the outputStream from your processor.
 * <p/>
 * Use {@link org.springframework.xd.reactor.MultipleBroadcasterMessageHandler} for concurrent execution on dispatcher
 * threads spread across across multiple Stream.
 * <p/>
 * All error handling is the responsibility of the processor implementation.
 *
 * @author Mark Pollack
 * @author Stephane Maldini
 */
public class BroadcasterMessageHandler extends AbstractMessageProducingHandler  implements DisposableBean {

    protected final Log logger = LogFactory.getLog(getClass());

    private final RingBufferProcessor<Object> ringBufferProcessor;

    private final Class<?> inputType;

    private int stopTimeout = 5000;

    private int ringBufferSize = 8192;

    /**
     * Construct a new BroadcasterMessageHandler given the reactor based Processor to delegate
     * processing to.
     *
     * @param processor The stream based reactor processor
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public BroadcasterMessageHandler(Processor processor) {
        Assert.notNull(processor, "processor cannot be null.");

        this.inputType = ReactorReflectionUtils.extractGeneric(processor);

        //Stream with a RingBufferProcessor
        this.ringBufferProcessor = RingBufferProcessor.share("xd-reactor", ringBufferSize);

        //user defined stream processing
        Publisher<?> outputStream = processor.process(Streams.wrap(ringBufferProcessor));

        outputStream.subscribe(new DefaultSubscriber<Object>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Object outputObject) {
                if (ClassUtils.isAssignable(Message.class, outputObject.getClass())) {
                    getOutputChannel().send((Message) outputObject);
                } else {
                    getOutputChannel().send(MessageBuilder.withPayload(outputObject).build());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                //Simple log error handling
                logger.error(throwable);
            }
        });
    }

    /**
     * Time in milliseconds to wait when shutting down the processor, waiting on a latch inside
     * the onComplete method of the subscriber.  Default is 5000 milliseconds
     * @param stopTimeoutInMillis time to wait when shutting down the processor.
     */
    public void setStopTimeout(int stopTimeoutInMillis) {
        this.stopTimeout = stopTimeoutInMillis;
    }

    /**
     * The size of the RingBuffer, must be a power of 2.  Default is 8192.
     *
     * @param ringBufferSize size of the RingBuffer.
     */
    public void setRingBufferSize(int ringBufferSize) {
        Assert.isTrue(ringBufferSize> 0 && Integer.bitCount(ringBufferSize) == 1,
                "'ringBufferSize' must be a power of 2 ");
        this.ringBufferSize = ringBufferSize;
    }

    @Override
    protected void handleMessageInternal(Message<?> message) throws Exception {

        if (inputType == null || ClassUtils.isAssignable(inputType, message.getClass())) {
            ringBufferProcessor.onNext(message);
        } else if (ClassUtils.isAssignable(inputType, message.getPayload().getClass())) {
            //TODO handle type conversion of payload to input type if possible
            ringBufferProcessor.onNext(message.getPayload());
        } else {
            throw new MessageHandlingException(message, "Processor signature does not match [" + message.getClass()
                    + "] or [" + message.getPayload().getClass() + "]");
        }
    }

    @Override
    public void destroy() throws Exception {
        if (ringBufferProcessor != null) {
            ringBufferProcessor.awaitAndShutdown(stopTimeout, TimeUnit.MILLISECONDS);
        }
    }


}
