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
 * and delegates processing to a RxJava Observable.  The number of observables that the message can be delegated
 * to is determined by the evaluation of the partitionExpression.
 *
 * For example, using the expression T(java.lang.Thread).currentThread().getId(), would map an instance of a
 * RxJava Observable to each dispatcher thread of the MessageBus.  If you wanted to have an Observable per
 * Kafka partition, the expression header['kafka_partition_id'].
 *
 * @author Mark Pollack
 */
public class MultipleSubjectMessageHandler extends AbstractMessageProducingHandler implements DisposableBean,
        IntegrationEvaluationContextAware {

    protected final Log logger = LogFactory.getLog(getClass());

    private final ConcurrentMap<Object, PublishSubject<Object>> subjectMap =
            new ConcurrentHashMap<Object, PublishSubject<Object>>();

    private final Map<Object, Subscription> subscriptionMap = new Hashtable<Object, Subscription>();

    @SuppressWarnings("rawtypes")
    private final Processor processor;

    private final ResolvableType inputType;

    private final Expression partitionExpression;

    private final SpelExpressionParser spelExpressionParser = new SpelExpressionParser();

    private EvaluationContext evaluationContext = new StandardEvaluationContext();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public MultipleSubjectMessageHandler(Processor processor, String partitionExpression) {
        Assert.notNull(processor, "processor cannot be null.");
        Assert.notNull(partitionExpression, "Partition expression can not be null");
        this.processor = processor;
        this.partitionExpression = spelExpressionParser.parseExpression(partitionExpression);
        Method method = ReflectionUtils.findMethod(this.processor.getClass(), "process", Observable.class);
        this.inputType = ResolvableType.forMethodParameter(method, 0).getNested(2);
    }


    @Override
    protected void handleMessageInternal(Message<?> message) throws Exception {
        Subject subjectToUse = getSubject(message);
        if (ClassUtils.isAssignable(inputType.getRawClass(), message.getClass())) {
            subjectToUse.onNext(message);
        } else if (ClassUtils.isAssignable(inputType.getRawClass(), message.getPayload().getClass())) {
            subjectToUse.onNext(message.getPayload());
        } else {
            throw new MessageHandlingException(message, "Processor signature does not match [" + message.getClass()
                    + "] or [" + message.getPayload().getClass() + "]");
        }
    }

    private Subject getSubject(Message message) {
        final Object idToUse = partitionExpression.getValue(evaluationContext, message, Object.class);
        if (logger.isDebugEnabled()) {
            logger.debug("Partition Expression evaluated to " + idToUse);
        }
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
                    subjectMap.remove(idToUse);
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

    @Override
    public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
        this.evaluationContext = evaluationContext;
    }
}