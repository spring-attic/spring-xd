/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.integration.hadoop.partition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Helper class to work with a spel expressions resolving values
 * from a {@link Message}. On default a {@link StandardEvaluationContext} is created with
 * a registered {@link MessagePartitionKeyMethodResolver} and {@link MessagePartitionKeyPropertyAccessor}.
 *
 *
 * @author Janne Valkealahti
 *
 */
public class MessageExpressionMethods {

	private final static Log logger = LogFactory.getLog(MessageExpressionMethods.class);

	private final StandardEvaluationContext context;

	/**
	 * Instantiates a new message expression methods.
	 */
	public MessageExpressionMethods() {
		this(true);
	}

	/**
	 * Instantiates a new message expression methods.
	 *
	 * @param register if method resolver and property accessor should be registered
	 */
	public MessageExpressionMethods(boolean register) {
		this(null, true);
	}

	/**
	 * Instantiates a new message expression methods.
	 *
	 * @param evaluationContext the evaluation context
	 */
	public MessageExpressionMethods(EvaluationContext evaluationContext) {
		this(evaluationContext, true);
	}

	/**
	 * Instantiates a new message expression methods with
	 * a {@link EvaluationContext} which is expected to be
	 * a {@link StandardEvaluationContext}.
	 *
	 * @param evaluationContext the spel evaluation context
	 * @param register if method resolver and property accessor should be registered
	 */
	public MessageExpressionMethods(EvaluationContext evaluationContext, boolean register) {
		StandardEvaluationContext context = null;
		if (evaluationContext instanceof StandardEvaluationContext) {
			context = (StandardEvaluationContext) evaluationContext;
		}
		else if (evaluationContext != null) {
			logger.warn("Expecting evaluation context of type StandardEvaluationContext, but it was "
					+ evaluationContext + " going to create a new StandardEvaluationContext");
		}
		if (context == null) {
			context = new StandardEvaluationContext();
		}
		if (register) {
			context.addMethodResolver(new MessagePartitionKeyMethodResolver());
			context.addPropertyAccessor(new MessagePartitionKeyPropertyAccessor());
		}
		this.context = context;
	}

	/**
	 * Gets the value.
	 *
	 * @param <T> the generic type
	 * @param expression the expression
	 * @param desiredResultType the desired result type
	 * @return the value
	 * @throws EvaluationException the evaluation exception
	 */
	public <T> T getValue(Expression expression, Message<?> message, Class<T> desiredResultType)
			throws EvaluationException {
		Assert.notNull(expression, "Expression cannot be null");
		return expression.getValue(context, message, desiredResultType);
	}

}
