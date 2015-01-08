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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.ResolvableType;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author Mark Pollack
 */
public class SubjectMessageHandler extends AbstractMessageProducingHandler {

    protected final Log logger = LogFactory.getLog(getClass());

    private final ConcurrentMap<Long, BehaviorSubject<Object>> subjectMap =
            new ConcurrentHashMap<Long, BehaviorSubject<Object>>();

    private final Object monitor = new Object();

    @SuppressWarnings("rawtypes")
    private final Processor processor;

    private final ResolvableType inputType;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public SubjectMessageHandler(Processor processor) {
        Assert.notNull(processor, "processor cannot be null.");
        this.processor = processor;
        Method method = ReflectionUtils.findMethod(this.processor.getClass(), "process", Observable.class);
        this.inputType = ResolvableType.forMethodParameter(method, 0).getNested(2);
    }


    @Override
    protected void handleMessageInternal(Message<?> message) throws Exception {
        Subject subjectToUse = getSubject();
        if (ClassUtils.isAssignable(inputType.getRawClass(), message.getClass())) {
            subjectToUse.onNext(message);
        } else if (ClassUtils.isAssignable(inputType.getRawClass(), message.getPayload().getClass())) {
            //TODO handle type conversion of payload to input type if possible
            System.out.println(Thread.currentThread().getName() +
                    "> Inside handleMessageInternal, sending to subject data = " + message.getPayload());
            subjectToUse.onNext(message.getPayload());
        }
        //TODO else log error.
    }

    private Subject getSubject() {
        long idToUse = Thread.currentThread().getId();
        Subject subject = subjectMap.get(idToUse);
        if (subject == null) {
            BehaviorSubject existingSubject = subjectMap.putIfAbsent(idToUse, BehaviorSubject.create());
            if (existingSubject == null)
                subject = subjectMap.get(idToUse);
            synchronized (this.monitor) {
                if (!subject.hasObservers()) {
                    //user defined stream processing
                    Observable<?> outputStream = processor.process(subject);

                    //TODO Error handling

                    final Subscription subscription = outputStream.subscribe(new Action1<Object>() {
                        @Override
                        public void call(Object outputObject) {
                            if (ClassUtils.isAssignable(Message.class, outputObject.getClass())) {
                                getOutputChannel().send((Message) outputObject);
                            } else {
                                getOutputChannel().send(MessageBuilder.withPayload(outputObject).build());
                            }
                            //System.out.println(Thread.currentThread().getName() + "> Going to send to message bus " + o);
                        }
                    });

                    //TODO keep track of subscriptions to unsubscribe cleanly...
                }
            }
        }
        return subject;

    }
}