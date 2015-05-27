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
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.expression.IntegrationEvaluationContextAware;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import reactor.Environment;
import reactor.core.processor.RingBufferProcessor;
import reactor.fn.Supplier;
import reactor.rx.Streams;
import reactor.rx.action.support.DefaultSubscriber;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Adapts the item at a time delivery of a {@link org.springframework.messaging.MessageHandler}
 * by delegating processing to a Stream based on a partitionExpression.
 * <p/>
 * The specific Stream that the message is delegated to is determined by the partitionExpression value.
 * Unless you change the scheduling of the inputStream in your processor, you should ensure that the
 * partitionExpression does not map messages delivered on different message bus dispatcher threads to the same
 * stream. This is due to the underlying use of a <code>Broadcaster</code>.
 * <p/>
 * For example, using the expression <code>T(java.lang.Thread).currentThread().getId()</code> would map the current
 * dispatcher thread id to an instance of a Stream. If you wanted to have a Stream per
 * Kafka partition, you can use the expression <code>header['kafka_partition_id']</code> since the MessageBus
 * dispatcher thread will be the same for each partition.
 * <p/>
 * If the Stream mapped to the partitionExpression value has an error or completes, it will be recreated when the
 * next message consumed maps to the same partitionExpression value.
 * <p/>
 * All error handling is the responsibility of the processor implementation.
 *
 * @author Mark Pollack
 * @author Stephane Maldini
 */
public class PartitionedReactorMessageHandler<IN, OUT> extends AbstractMessageProducingHandler implements
    DisposableBean,
		IntegrationEvaluationContextAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private final ConcurrentMap<Object, RingBufferProcessor<IN>> reactiveProcessorMap =
			new ConcurrentHashMap<>();

	private final ReactiveProcessor<IN, OUT> processor;

	private final Class<IN> inputType;

	private final Expression partitionExpression;

	private final Environment environment;

	private EvaluationContext evaluationContext = new StandardEvaluationContext();

	/**
	 * Construct a new SynchronousDispatcherMessageHandler given the reactor based Processor to delegate
	 * processing to.
	 *
	 * @param processor The stream based reactor processor
	 */
	public PartitionedReactorMessageHandler(ReactiveProcessor<IN, OUT> processor,
	                                        Class<IN> inputType,
	                                        String partitionExpression) {
		Assert.notNull(processor, "processor cannot be null.");
		Assert.notNull(partitionExpression, "Partition expression can not be null");
		this.processor = processor;

		SpelExpressionParser spelExpressionParser = new SpelExpressionParser();
		this.partitionExpression = spelExpressionParser.parseExpression(partitionExpression);

		// This by default create no dispatcher but provides for Timer if buffer(1, TimeUnit.Seconds) or similar is used
		this.environment = Environment.initializeIfEmpty();

		this.inputType = inputType;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void handleMessageInternal(Message<?> message) {
		RingBufferProcessor<IN> reactiveProcessorToUse = getReactiveProcessor(message);
		if (inputType == null || ClassUtils.isAssignable(inputType, message.getClass())) {
			reactiveProcessorToUse.onNext((IN) message);
		} else {
			reactiveProcessorToUse.onNext((IN) message.getPayload());
		}
	}

	@SuppressWarnings("unchecked")
	private RingBufferProcessor<IN> getReactiveProcessor(Message<?> message) {
		final Object idToUse = partitionExpression.getValue(evaluationContext, message, Object.class);
		if (logger.isDebugEnabled()) {
			logger.debug("Partition Expression evaluated to " + idToUse);
		}
		RingBufferProcessor<IN> reactiveProcessor = reactiveProcessorMap.get(idToUse);
		if (reactiveProcessor == null) {
			RingBufferProcessor<IN> newReactiveProcessor =
					RingBufferProcessor.share("xd-reactor-partition-" + idToUse, 64);

			RingBufferProcessor<IN> existingReactiveProcessor =
					reactiveProcessorMap.putIfAbsent(idToUse, newReactiveProcessor);
			if (existingReactiveProcessor == null) {
				reactiveProcessor = reactiveProcessorMap.get(idToUse);
				//user defined stream processing
				processor.accept(Streams.wrap(reactiveProcessor), new Supplier<Subscriber<OUT>>() {
					@Override
					public Subscriber<OUT> get() {
						return new DefaultSubscriber<OUT>() {
							Subscription s;

							@Override
							public void onSubscribe(Subscription s) {
								this.s = s;
								s.request(Long.MAX_VALUE);
								if (logger.isDebugEnabled()) {
									logger.debug("xd-reactor started [ " + PartitionedReactorMessageHandler.this + " - " + s + " ]");
								}
							}

							@Override
							public void onNext(OUT outputObject) {
								if (ClassUtils.isAssignable(Message.class, outputObject.getClass())) {
									getOutputChannel().send((Message) outputObject);
								} else {
									getOutputChannel().send(MessageBuilder.withPayload(outputObject).build());
								}
							}

							@Override
							public void onError(Throwable throwable) {
								logger.error("", throwable);
								reactiveProcessorMap.remove(idToUse);
								if (s != null) {
									s.cancel();
								}
							}

							@Override
							public void onComplete() {
								if (logger.isDebugEnabled()) {
									logger.debug("xd-reactor completed [ " + PartitionedReactorMessageHandler.this + " - " + s + " ]");

								}
								reactiveProcessorMap.remove(idToUse);
								if (s != null) {
									s.cancel();
								}
							}
						};
					}
				});
			} else {
				newReactiveProcessor.onComplete();
				reactiveProcessor = existingReactiveProcessor;
			}
		}
		return reactiveProcessor;
	}

	@Override
	public void destroy() throws Exception {
		Collection<RingBufferProcessor<IN>> toRemove =
				new ArrayList<>(reactiveProcessorMap.values());

		for (RingBufferProcessor<IN> ringBufferProcessor : toRemove) {
			ringBufferProcessor.onComplete();
		}

		environment.shutdown();
	}

	@Override
	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}
}
