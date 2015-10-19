/*
 * Copyright 2015 the original author or authors.
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

import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import reactor.Environment;
import reactor.core.processor.RingBufferProcessor;
import reactor.rx.action.support.DefaultSubscriber;

/**
 * Abstract base class for Reactor based MessageHandlers.
 *
 * @author Mark Pollack
 */
public abstract class AbstractReactorMessageHandler extends AbstractMessageProducingHandler implements DisposableBean {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private int stopTimeout = 5000;

    private int ringBufferSize = 8192;

    private final Environment environment = new Environment().assignErrorJournal();

    private final Class<?> inputType;

    @SuppressWarnings("rawtypes")
    protected final Processor processor;

    /**
     * Construct a new BroadcasterMessageHandler given the reactor based Processor to delegate
     * processing to.
     *
     * @param processor The stream based reactor processor
     */
    @SuppressWarnings("rawtypes")
    public AbstractReactorMessageHandler(Processor processor) {
        Assert.notNull(processor, "processor cannot be null.");
        this.processor = processor;
        this.inputType = ReactorReflectionUtils.extractInputType(processor);
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
     * Return the time to wait when shutting down the processor in millseconds
     */
    public int getStopTimeout() {
        return stopTimeout;
    }


    /**
     * The size of the RingBuffer, must be a power of 2.  Default is 8192.
     *
     * @param ringBufferSize size of the RingBuffer.
     */
    public void setRingBufferSize(int ringBufferSize) {
        Assert.isTrue(ringBufferSize > 0 && Integer.bitCount(ringBufferSize) == 1,
                "'ringBufferSize' must be a power of 2 ");
        this.ringBufferSize = ringBufferSize;
    }

    /**
     * Return the size of the RingBuffer
     */
    public int getRingBufferSize() {
        return ringBufferSize;
    }

    /**
     * Return the environment to use for stream processing operations.
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * Returns the input type of the stream that is to be processed
     */
    public Class<?> getInputType() {
        return inputType;
    }


    protected void invokeProcessor(Message<?> message, RingBufferProcessor<Object> reactiveProcessorToUse) {
        // pass the message directly if the input type accepts it, unless the input type is Object
        // this restricts the branch to Message and its subinterfaces/implementations
        if (!Object.class.equals(inputType) && ClassUtils.isAssignable(inputType, message.getClass())) {
            reactiveProcessorToUse.onNext(message);
        } else if (ClassUtils.isAssignable(inputType, message.getPayload().getClass())) {
            reactiveProcessorToUse.onNext(message.getPayload());
        } else {
            throw new MessageHandlingException(message, "Processor signature does not match [" + message.getClass()
                + "] or [" + message.getPayload().getClass() + "]");
        }
    }


    protected class ChannelForwardingSubscriber extends DefaultSubscriber<Object> {
        Subscription s;

        @Override
        public void onSubscribe(Subscription s) {
            this.s = s;
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
            logger.error("Error processing stream [" + s + "]", throwable);
        }

        @Override
        public void onComplete() {
            logger.info("Consumer completed for [" + s + "]");
        }
    }
}
