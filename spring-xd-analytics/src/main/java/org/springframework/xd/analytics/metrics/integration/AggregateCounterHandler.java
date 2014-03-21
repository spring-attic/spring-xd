/*
 * Copyright 2011-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.analytics.metrics.integration;

import java.text.ParseException;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.AggregateCounterRepository;

/**
 * Service activator for the {@code aggregate-counter} module.
 * 
 * @author Luke Taylor
 * @author Eric Bottard
 */
public class AggregateCounterHandler implements BeanFactoryAware, InitializingBean {

	private static Logger logger = LoggerFactory.getLogger(AggregateCounterHandler.class);

	private final AggregateCounterRepository aggregateCounterRepository;

	private final String counterName;

	private ExpressionEvaluatingMessageProcessor<Object> processor;

	private BeanFactory beanFactory;

	public AggregateCounterHandler(AggregateCounterRepository aggregateCounterRepository, String counterName) {
		Assert.notNull(aggregateCounterRepository, "Aggregate Counter Repository can not be null");
		Assert.notNull(counterName, "Counter Name can not be null");
		this.aggregateCounterRepository = aggregateCounterRepository;
		this.counterName = counterName;
	}

	public void setExpression(String expressionString) {
		Expression expression = new SpelExpressionParser().parseExpression(expressionString);
		this.processor = new ExpressionEvaluatingMessageProcessor<Object>(expression);
	}


	public Message<?> process(Message<?> message) throws ParseException {
		if (message == null) {
			return null;
		}
		else {
			Object timestampObject = processor.processMessage(message);
			DateTime timestamp = null;
			if (timestampObject instanceof DateTime) {
				timestamp = (DateTime) timestampObject;
			}
			else {
				try {
					timestamp = new DateTime(timestampObject);
				}
				catch (IllegalArgumentException e) {
					logger.debug("Could not convert result of expression to a DateTime: {}", timestampObject);
				}
			}
			this.aggregateCounterRepository.increment(counterName, 1, timestamp);
		}
		return message;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.beanFactory != null) {
			this.processor.setBeanFactory(beanFactory);
			this.processor.afterPropertiesSet();
		}
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}
}
