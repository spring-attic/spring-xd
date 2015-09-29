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

import java.util.concurrent.TimeUnit;

import org.reactivestreams.Publisher;

import org.springframework.messaging.Message;

import reactor.core.processor.RingBufferProcessor;
import reactor.rx.Stream;
import reactor.rx.Streams;

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
 * @author Stephane Malbdini
 */
public class BroadcasterMessageHandler extends AbstractReactorMessageHandler {

    private RingBufferProcessor<Object> ringBufferProcessor;

    /**
     * Construct a new BroadcasterMessageHandler given the reactor based Processor to delegate
     * processing to.
     *
     * @param processor The stream based reactor processor
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public BroadcasterMessageHandler(Processor processor) {
        super(processor);
    }

    @Override
    protected void handleMessageInternal(Message<?> message) throws Exception {
        invokeProcessor(message, ringBufferProcessor);
    }

    @Override
    public void destroy() throws Exception {
        if (ringBufferProcessor != null) {
            ringBufferProcessor.awaitAndShutdown(getStopTimeout(), TimeUnit.MILLISECONDS);
        }
        getEnvironment().shutdown();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onInit() throws Exception {
        super.onInit();
        //Stream with a RingBufferProcessor
        this.ringBufferProcessor = RingBufferProcessor.share("xd-reactor", getRingBufferSize());

        //user defined stream processing
        Publisher<?> outputStream = processor.process(Streams.wrap(ringBufferProcessor).env(getEnvironment()));

        outputStream.subscribe(new ChannelForwardingSubscriber());
    }
}
