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
import rx.subjects.Subject;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A handler that adapts the item at a time delivery in a {@link org.springframework.messaging.MessageHandler}
 * and delegates processing to an RxJava Observable.  A
 *
 * @author Mark Pollack
 */
public class SubjectMessageHandler extends AbstractMessageProducingHandler implements DisposableBean {

    protected final Log logger = LogFactory.getLog(getClass());

    private final ConcurrentMap<Long, PublishSubject<Object>> subjectMap =
            new ConcurrentHashMap<Long, PublishSubject<Object>>();

    private final Map<Long, Subscription> subscriptionMap = new Hashtable<Long, Subscription>();

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
            subjectToUse.onNext(message.getPayload());
        } else {
            throw new MessageHandlingException(message, "Processor signature does not match [" + message.getClass()
                    + "] or [" + message.getPayload().getClass() + "]");
        }
    }

    private Subject getSubject() {
        long idToUse = Thread.currentThread().getId();
        Subject subject = subjectMap.get(idToUse);
        if (subject == null) {
            PublishSubject existingSubject = subjectMap.putIfAbsent(idToUse, PublishSubject.create());
            if (existingSubject == null)
                subject = subjectMap.get(idToUse);
            //user defined stream processing
            Observable<?> outputStream = processor.process(subject);

            final Subscription subscription = outputStream.subscribe(new Action1<Object>() {
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
                    logger.error(throwable);
                }
            }, new Action0() {
                @Override
                public void call() {
                    subjectMap.remove(Thread.currentThread().getId());
                }
            });

            subscriptionMap.put(idToUse, subscription);


        }
        return subject;

    }

    @Override
    public void destroy() throws Exception {
        for (Subscription subscription : subscriptionMap.values()) {
            subscription.unsubscribe();
        }
    }
}