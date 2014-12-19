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
import org.springframework.core.ResolvableType;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.messaging.Message;
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

import java.lang.SuppressWarnings;
import java.lang.reflect.Method;

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
public class SynchronousDispatcherMessageHandler extends AbstractMessageProducingHandler {

    protected final Log logger = LogFactory.getLog(getClass());

    private final Broadcaster<Object> stream;

	@SuppressWarnings("rawtypes")
    private final Processor reactorProcessor;

    private final ResolvableType inputType;

    private final Controls consume;

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
        Environment.initializeIfEmpty(); // This by default uses SynchronousDispatcher
        Method method = ReflectionUtils.findMethod(this.reactorProcessor.getClass(), "process", Stream.class);
        this.inputType = ResolvableType.forMethodParameter(method, 0).getNested(2);

        //Stream with a SynchronousDispatcher as this handler is called by Message Listener managed threads
        this.stream = Streams.broadcast();

        //user defined stream processing
        Stream<?> outputStream = processor.process(stream);

        //Simple log error handling
        outputStream.when(Throwable.class, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                logger.error(throwable);
            }
        });

        this.consume = outputStream.consume(new Consumer<Object>() {
            @Override
            public void accept(Object outputObject) {
                if (ClassUtils.isAssignable(Message.class, outputObject.getClass())) {
                    getOutputChannel().send((Message) outputObject);
                } else {
                    //TODO handle copy of header values when possible
                    getOutputChannel().send(MessageBuilder.withPayload(outputObject).build());
                }
                //TODO handle Void
            }
        });

        if (logger.isDebugEnabled()) {
            logger.debug(consume.debug());
        }

    }

    @Override
    protected void handleMessageInternal(Message<?> message) throws Exception {

        if (ClassUtils.isAssignable(inputType.getRawClass(), message.getClass())) {
            stream.onNext(message);
        } else if (ClassUtils.isAssignable(inputType.getRawClass(), message.getPayload().getClass())) {
            //TODO handle type conversion of payload to input type if possible
            stream.onNext(message.getPayload());
        }

        if (logger.isDebugEnabled()) {
            logger.debug(consume.debug());
        }
    }

}
