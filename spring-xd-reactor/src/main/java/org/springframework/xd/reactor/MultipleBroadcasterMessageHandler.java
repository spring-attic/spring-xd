/*
 * Copyright 2014-2015 the original author or authors.
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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.Publisher;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import reactor.core.processor.RingBufferProcessor;
import reactor.rx.Streams;

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
 * @author Gary Russell
 */
public class MultipleBroadcasterMessageHandler extends AbstractReactorMessageHandler {

    private final ConcurrentMap<Object, RingBufferProcessor<Object>> reactiveProcessorMap =
            new ConcurrentHashMap<Object, RingBufferProcessor<Object>>();

    private final Expression partitionExpression;

    private final SpelExpressionParser spelExpressionParser = new SpelExpressionParser();

    private EvaluationContext evaluationContext = new StandardEvaluationContext();

    private boolean evaluationContextSet;

    /**
     * Construct a new MessageHandler given the reactor based Processor to delegate
     * processing to and a partition expression.
     *
     * @param processor The stream based reactor processor
     */
    @SuppressWarnings({ "rawtypes" })
    public MultipleBroadcasterMessageHandler(Processor processor, String partitionExpression) {
        super(processor);
        Assert.notNull(partitionExpression, "Partition expression can not be null");
        this.partitionExpression = spelExpressionParser.parseExpression(partitionExpression);
    }

    public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
        this.evaluationContext = evaluationContext;
        this.evaluationContextSet = true;
    }


    @Override
    protected void onInit() throws Exception {
        if (!this.evaluationContextSet) {
            this.evaluationContext = IntegrationContextUtils.getEvaluationContext(getBeanFactory());
        }
    }

    @Override
    protected void handleMessageInternal(Message<?> message) {
        RingBufferProcessor<Object> reactiveProcessorToUse = getReactiveProcessor(message);
        invokeProcessor(message, reactiveProcessorToUse);
    }

    @SuppressWarnings("unchecked")
    private RingBufferProcessor<Object> getReactiveProcessor(Message<?> message) {
        final Object idToUse = partitionExpression.getValue(evaluationContext, message, Object.class);
        if (logger.isDebugEnabled()) {
            logger.debug("Partition Expression evaluated to " + idToUse);
        }
        RingBufferProcessor<Object> reactiveProcessor = reactiveProcessorMap.get(idToUse);
        if (reactiveProcessor == null) {
            RingBufferProcessor<Object> existingReactiveProcessor =
                    reactiveProcessorMap.putIfAbsent(idToUse,
                            RingBufferProcessor.share("xd-reactor-partition-" + idToUse, getRingBufferSize()));
            if (existingReactiveProcessor == null) {
                reactiveProcessor = reactiveProcessorMap.get(idToUse);
                //user defined stream processing
                Publisher<?> outputStream = processor.process(Streams.wrap(reactiveProcessor).env(getEnvironment()));

                outputStream.subscribe(new ChannelForwardingSubscriber());

            }
            else {
                reactiveProcessor = existingReactiveProcessor;
            }
        }
        return reactiveProcessor;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void destroy() throws Exception {
        for (RingBufferProcessor ringBufferProcessor : reactiveProcessorMap.values()) {
            ringBufferProcessor.awaitAndShutdown(getStopTimeout(), TimeUnit.MILLISECONDS);
        }
        getEnvironment().shutdown();
    }

}
