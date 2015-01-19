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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.ResolvableType;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.expression.IntegrationEvaluationContextAware;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import reactor.Environment;
import reactor.fn.Consumer;
import reactor.rx.Controls;
import reactor.rx.Stream;
import reactor.rx.Streams;
import reactor.rx.stream.Broadcaster;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Adapts the item at a time delivery of a {@link org.springframework.messaging.MessageHandler}
 * by delegating processing to an Stream based on a partitionExpression.
 * <p/>
 * The specific Stream that the message is delegated is determined by the partitionExpression value.
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
 */
public class MultipleBroadcasterMessageHandler extends AbstractMessageProducingHandler implements DisposableBean,
        IntegrationEvaluationContextAware {

    protected final Log logger = LogFactory.getLog(getClass());

    private final ConcurrentMap<Object, Broadcaster<Object>> broadcasterMap =
            new ConcurrentHashMap<Object, Broadcaster<Object>>();

    private final Map<Object, Controls> controlsMap = new Hashtable<Object, Controls>();

    private final Environment environment;

    @SuppressWarnings("rawtypes")
    private final Processor processor;

    private final ResolvableType inputType;

    private final Expression partitionExpression;

    private final SpelExpressionParser spelExpressionParser = new SpelExpressionParser();

    private EvaluationContext evaluationContext = new StandardEvaluationContext();

    /**
     * Construct a new SynchronousDispatcherMessageHandler given the reactor based Processor to delegate
     * processing to.
     *
     * @param processor The stream based reactor processor
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MultipleBroadcasterMessageHandler(Processor processor, String partitionExpression) {
        Assert.notNull(processor, "processor cannot be null.");
        Assert.notNull(partitionExpression, "Partition expression can not be null");
        this.processor = processor;
        this.partitionExpression = spelExpressionParser.parseExpression(partitionExpression);
        environment = Environment.initializeIfEmpty(); // This by default uses SynchronousDispatcher
        Method method = ReflectionUtils.findMethod(this.processor.getClass(), "process", Stream.class);
        this.inputType = ResolvableType.forMethodParameter(method, 0).getNested(2);
    }

    @Override
    protected void handleMessageInternal(Message<?> message) {
        Broadcaster<Object> broadcasterToUse = getBroadcaster(message);
        if (ClassUtils.isAssignable(inputType.getRawClass(), message.getClass())) {
            broadcasterToUse.onNext(message);
        } else if (ClassUtils.isAssignable(inputType.getRawClass(), message.getPayload().getClass())) {
            broadcasterToUse.onNext(message.getPayload());
        } else {
            throw new MessageHandlingException(message, "Processor signature does not match [" + message.getClass()
                    + "] or [" + message.getPayload().getClass() + "]");
        }

        if (logger.isDebugEnabled()) {
            Controls controls = this.controlsMap.get(Thread.currentThread().getId());
            if (controls != null) {
                logger.debug(controls.debug());
            }
        }
    }


    private Broadcaster<Object> getBroadcaster(Message<?> message) {
        final Object idToUse = partitionExpression.getValue(evaluationContext, message, Object.class);
        if (logger.isDebugEnabled()) {
            logger.debug("Partition Expression evaluated to " + idToUse);
        }
        Broadcaster<Object> broadcaster = broadcasterMap.get(idToUse);
        if (broadcaster == null) {
            Broadcaster<Object> existingBroadcaster = broadcasterMap.putIfAbsent(idToUse, Streams.broadcast());
            if (existingBroadcaster == null) {
                broadcaster = broadcasterMap.get(idToUse);
                //user defined stream processing
                Stream<?> outputStream = processor.process(broadcaster);

                final Controls controls = outputStream.consume(new Consumer<Object>() {
                    @Override
                    public void accept(Object outputObject) {
                        if (ClassUtils.isAssignable(Message.class, outputObject.getClass())) {
                            getOutputChannel().send((Message) outputObject);
                        } else {
                            getOutputChannel().send(MessageBuilder.withPayload(outputObject).build());
                        }
                    }
                });

                outputStream.when(Throwable.class, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        logger.error(throwable);
                        broadcasterMap.remove(idToUse);
                    }
                });

                broadcaster.observeComplete(new Consumer<Void>() {
                    @Override
                    public void accept(Void aVoid) {
                        logger.error("Consumer completed for [" + controls + "]");
                        broadcasterMap.remove(idToUse);
                    }
                });


                controlsMap.put(idToUse, controls);



                if (logger.isDebugEnabled()) {
                    logger.debug(controls.debug());
                }
            } else {
                broadcaster = existingBroadcaster;
            }
        }
        return broadcaster;
    }

    @Override
    public void destroy() throws Exception {
        for (Controls controls : controlsMap.values()) {
            controls.cancel();
        }
        environment.shutdown();
    }

    @Override
    public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
        this.evaluationContext = evaluationContext;
    }
}
