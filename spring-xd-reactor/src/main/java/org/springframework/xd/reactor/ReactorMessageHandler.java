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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.processor.ReactorProcessor;
import reactor.core.processor.RingBufferProcessor;
import reactor.core.processor.RingBufferWorkProcessor;
import reactor.jarjar.com.lmax.disruptor.LiteBlockingWaitStrategy;
import reactor.rx.Stream;
import reactor.rx.Streams;
import reactor.rx.action.support.DefaultSubscriber;

import java.util.concurrent.TimeUnit;

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
public final class ReactorMessageHandler<IN, OUT> extends AbstractReactorMessageHandler<IN, OUT> {

	private final ReactorProcessor<IN, IN> reactorProcessor;

	/**
	 * Construct a new {@link ReactorMessageHandler} given the reactor based Processor to delegate
	 * processing to.
	 *
	 * @param processor The stream based reactor processor
	 */
	public ReactorMessageHandler(ReactiveProcessor<IN, OUT> processor,
	                             MessageChannel output,
	                             Class<IN> inputType,
	                             Class<OUT> outputType,
	                             int concurrency,
	                             int backlog,
	                             long timeout
	) {
		super(processor, output, inputType, outputType, concurrency, backlog, timeout);

		//Stream with a  RingBufferProcessor
		if (inputType == null) {
			this.reactorProcessor = null;
		} else {
			this.reactorProcessor = this.concurrency == 1 ?
					RingBufferProcessor.<IN>share("xd-reactor", backlog) :
					RingBufferWorkProcessor.<IN>share("xd-reactor", backlog, new LiteBlockingWaitStrategy());
		}
	}

	@Override
	protected void doStart() {
		processor.accept(reactorProcessor != null ? Streams.wrap(reactorProcessor) : null, new ReactiveOutput<OUT>() {
			@Override
			public void writeOutput(Publisher<? extends OUT> writeOutput) {
				writeOutput.subscribe(new DefaultSubscriber<OUT>() {
					@Override
					public void onSubscribe(final Subscription s) {
						s.request(Long.MAX_VALUE);
						if (logger.isDebugEnabled()) {
							logger.debug("xd-reactor started [ " + ReactorMessageHandler.this + " ]");
						}
					}

					@Override
					public void onNext(OUT outputObject) {
						forwardToOutput(outputObject);
					}

					@Override
					public void onError(Throwable throwable) {
						//Simple log error handling
						logger.error("", throwable);
					}

					@Override
					public void onComplete() {
						if (logger.isDebugEnabled()) {
							logger.debug("xd-reactor completed [ " + ReactorMessageHandler.this + " ]");
						}
					}
				});
			}
		});
	}

	@Override
	protected ReactorProcessor<IN, ?> resolveProcessor(Message<?> message){
		if (reactorProcessor == null) {
			throw new MessagingException("This ReactiveModule does not accept input messages");
		}
		return reactorProcessor;
	}

	@Override
	public boolean isRunning() {
		return reactorProcessor.alive();
	}

	@Override
	protected void doShutdown(long timeout) {
		if (reactorProcessor != null) {
			reactorProcessor.awaitAndShutdown(timeout, TimeUnit.MILLISECONDS);
		}
	}
}
