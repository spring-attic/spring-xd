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
 * A handler that adapts item at a time delivery in a
 * {@link org.springframework.messaging.MessageHandler}
 * and delegates processing to a Reactor Stream with synchronous dispatch  so that processing
 * occurs on the same thread that invokes the handler.
 * <p/>
 * The output stream of the processor is consumed and sent to the output channel.
 *
 * @author Mark Pollack
 */
public class SynchronousDispatcherMessageHandler extends AbstractMessageProducingHandler implements DisposableBean {

    protected final Log logger = LogFactory.getLog(getClass());

    private final ConcurrentMap<Long, Broadcaster<Object>> broadcasterMap =
            new ConcurrentHashMap<Long, Broadcaster<Object>>();

    private final Map<Long, Controls> controlsMap = new Hashtable<Long, Controls>();

    private final Environment environment;

    @SuppressWarnings("rawtypes")
    private final Processor reactorProcessor;

    private final ResolvableType inputType;

    /**
     * Construct a new SynchronousDispatcherMessageHandler given the reactor based Processor to delegate
     * processing to.
     *
     * @param processor The stream based reactor processor
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public SynchronousDispatcherMessageHandler(Processor processor) {
        Assert.notNull(processor, "processor cannot be null.");
        this.reactorProcessor = processor;
        environment = Environment.initializeIfEmpty(); // This by default uses SynchronousDispatcher
        Method method = ReflectionUtils.findMethod(this.reactorProcessor.getClass(), "process", Stream.class);
        this.inputType = ResolvableType.forMethodParameter(method, 0).getNested(2);
    }

    @Override
    protected void handleMessageInternal(Message<?> message) {
        Broadcaster<Object> broadcasterToUse = getBroadcaster();
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


    private Broadcaster<Object> getBroadcaster() {
        long idToUse = Thread.currentThread().getId();
        Broadcaster<Object> broadcaster = broadcasterMap.get(idToUse);

        if (broadcaster == null) {
            Broadcaster<Object> existingBroadcaster = broadcasterMap.putIfAbsent(idToUse, Streams.broadcast());
            if (existingBroadcaster == null) {
                broadcaster = broadcasterMap.get(idToUse);
                //user defined stream processing
                Stream<?> outputStream = reactorProcessor.process(broadcaster);
                //Simple log error handling
                outputStream.when(Throwable.class, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        logger.error(throwable);
                    }
                });

                Controls controls = outputStream.consume(new Consumer<Object>() {
                    @Override
                    public void accept(Object outputObject) {
                        if (ClassUtils.isAssignable(Message.class, outputObject.getClass())) {
                            getOutputChannel().send((Message) outputObject);
                        } else {
                            getOutputChannel().send(MessageBuilder.withPayload(outputObject).build());
                        }
                    }
                });

                controlsMap.put(idToUse, controls);

                broadcaster.observeComplete(new Consumer<Void>() {
                    @Override
                    public void accept(Void aVoid) {
                        broadcasterMap.remove(Thread.currentThread().getId());
                    }
                });

                if (logger.isDebugEnabled()) {
                    logger.debug(controls.debug());
                }
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
}
