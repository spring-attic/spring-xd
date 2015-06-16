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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.Lifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import reactor.Environment;
import reactor.core.processor.ReactorProcessor;
import reactor.core.processor.RingBufferProcessor;
import reactor.rx.Stream;

/**
 * Adapts the item at a time delivery of a {@link MessageHandler}
 * by delegating processing to a {@link Stream}
 * <p/>
 * The outputStream of the processor is used to create a message and send it to the output channel. If the
 * input channel and output channel are connected to the MessageBus, then data delivered to the input stream via
 * a call to onNext is invoked on the dispatcher thread of the message bus and sending a message to the output
 * channel will involve IO operations on the message bus.
 * <p/>
 * The implementation uses a {@link RingBufferProcessor} with asynchronous dispatch.
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
 * will use by explicitly calling <code>dispatchOn</code> or other switch (http://projectreactor
 * .io/docs/reference/#streams-multithreading)
 * before returning the outputStream from your processor.
 * <p/>
 * Use {@link PartitionedReactorMessageHandler} for concurrent execution on dispatcher
 * threads spread across across multiple Stream.
 * <p/>
 * All error handling is the responsibility of the processor implementation.
 *
 * @author Mark Pollack
 * @author Stephane Maldini
 */
public abstract class AbstractReactorMessageHandler<IN, OUT> implements Lifecycle, MessageHandler {

	protected final Log logger = LogFactory.getLog(getClass());

	protected final Environment environment;

	protected final ReactiveProcessor<IN, OUT> processor;

	protected final MessageChannel output;

	protected final int concurrency;

	protected final Class<IN> inputType;

	protected final Class<OUT> outputType;

	protected final int backlog;

	protected final long shutdownTimeout;

	/**
	 * Construct a new {@link AbstractReactorMessageHandler} given the reactor based Processor to delegate
	 * processing to.
	 *
	 * @param processor The stream based reactor processor
	 */
	public AbstractReactorMessageHandler(ReactiveProcessor<IN, OUT> processor,
	                                     MessageChannel output,
	                                     Class<IN> inputType,
	                                     Class<OUT> outputType,
	                                     int concurrency,
	                                     int backlog,
	                                     long shutdownTimeout
	) {
		Assert.notNull(processor, "Processor cannot be null.");
		Assert.isTrue(concurrency >= 0, "Cannot set negative concurrency: " + concurrency);
		Assert.isTrue(backlog > 0, "Cannot set negative or null backlog size: " + backlog);
		Assert.isTrue(Integer.bitCount(backlog) == 1, "Backlog must be a power of 2: " + backlog);

		environment = Environment.initializeIfEmpty(); // This by default uses SynchronousDispatcher

		this.output = output;
		this.backlog = backlog;
		this.shutdownTimeout = shutdownTimeout;

		this.inputType = inputType == null || Message.class.isAssignableFrom(inputType) ? null : inputType;
		this.outputType = outputType == null || Message.class.isAssignableFrom(outputType) ? null : outputType;

		this.concurrency = concurrency == 0 ? Runtime.getRuntime().availableProcessors() : concurrency;
		this.processor = processor;
	}

	@Override
	public final void start() {
		//user defined stream processing
		for (int i = 0; i < concurrency; i++) {
			doStart();
		}
	}

	protected abstract void doStart();

	@Override
	public final void stop() {
		doShutdown(-1);
		environment.shutdown();
	}

	protected abstract void doShutdown(long timeout);

	protected abstract ReactorProcessor<IN, ?> resolveProcessor(Message<?> message);

	@SuppressWarnings("unchecked")
	public final void handleMessage(Message<?> message) {
		ReactorProcessor<IN, ?> reactorProcessor = resolveProcessor(message);

		if (inputType == null) {
			reactorProcessor.onNext((IN) message);
		} else {
			//TODO handle type conversion of payload to input type if possible
			reactorProcessor.onNext((IN) message.getPayload());
		}
	}

	@SuppressWarnings("unchecked")
	protected final void forwardToOutput(OUT outputObject) {
		if (outputType == null) {
			output.send((Message) outputObject);
		} else {
			output.send(MessageBuilder.withPayload(outputObject).build());
		}
	}
}
