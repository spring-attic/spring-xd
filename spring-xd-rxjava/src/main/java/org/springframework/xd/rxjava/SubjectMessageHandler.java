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
package org.springframework.xd.rxjava;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.ResolvableType;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

/**
 * Adapts the item at a time delivery of a {@link org.springframework.messaging.MessageHandler}
 * by delegating processing to a {@link Observable}.
 * <p/>
 * The outputStream of the processor is used to create a message and send it to the output channel.  If the
 * input channel and output channel are connected to the MessageBus, then data delivered to the input stream via
 * a call to onNext is invoked on the dispatcher thread of the message bus and sending a message to the output
 * channel will involve IO operations on the message bus.
 * <p/>
 * The implementation uses a SerializedSubject.  This has the advantage that the state of the Observabale
 * can be shared across all the incoming dispatcher threads that are invoking onNext.  It has the disadvantage
 * that processing and sending to the output channel will execute serially on one of the dispatcher threads.
 * <p/>
 * The use of this handler makes for a very natural first experience when processing data.  For example given
 * the stream <code></code>http | rxjava-processor | log</code> where the <code>rxjava-processor</code> does does a
 * <code>buffer(5)</code> and then produces a single value.  Sending 10 messages to the http source will
 * result in 2 messages in the log, no matter how many dispatcher threads are used.
 * <p/>
 * You can modify what thread the outputStream subscriber, which does the send to the output channel,
 * will use by explicitly calling <code>observeOn</code> before returning the outputStream from your processor.
 * <p/>
 * Use {@link org.springframework.xd.rxjava.MultipleSubjectMessageHandler} for concurrent execution on dispatcher
 * threads spread across across multiple Observables.

 * All error handling is the responsibility of the processor implementation.
 *
 * @author Mark Pollack
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class SubjectMessageHandler extends AbstractMessageProducingHandler implements DisposableBean {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConcurrentMap<Long, PublishSubject<Object>> subjectMap =
            new ConcurrentHashMap<Long, PublishSubject<Object>>();

    private final Map<Long, Subscription> subscriptionMap = new Hashtable<Long, Subscription>();

    @SuppressWarnings("rawtypes")
    private final Processor processor;

    private final ResolvableType inputType;

    private final Subject subject;

    private final Subscription subscription;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public SubjectMessageHandler(Processor processor) {
        Assert.notNull(processor, "processor cannot be null.");
        this.processor = processor;
        Method method = ReflectionUtils.findMethod(this.processor.getClass(), "process", Observable.class);
        this.inputType = ResolvableType.forMethodParameter(method, 0).getNested(2);
        subject = new SerializedSubject(PublishSubject.create());
        Observable<?> outputStream = processor.process(subject);
        subscription = outputStream.subscribe(new Action1<Object>() {
            @Override
            public void call(Object outputObject) {
                if (ClassUtils.isAssignable(Message.class, outputObject.getClass())) {
                    getOutputChannel().send((Message) outputObject);
                } else {
                    getOutputChannel().send(MessageBuilder.withPayload(outputObject).build());
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                logger.error(throwable.getMessage(), throwable);
            }
        }, new Action0() {
            @Override
            public void call() {
                logger.error("Subscription close for [" + subscription + "]");
            }
        });
    }


    @Override
    protected void handleMessageInternal(Message<?> message) throws Exception {
        if (ClassUtils.isAssignable(inputType.getRawClass(), message.getClass())) {
            subject.onNext(message);
        } else if (ClassUtils.isAssignable(inputType.getRawClass(), message.getPayload().getClass())) {
            subject.onNext(message.getPayload());
        } else {
            throw new MessageHandlingException(message, "Processor signature does not match [" + message.getClass()
                    + "] or [" + message.getPayload().getClass() + "]");
        }
    }

    @Override
    public void destroy() throws Exception {
        subscription.unsubscribe();
    }
}
