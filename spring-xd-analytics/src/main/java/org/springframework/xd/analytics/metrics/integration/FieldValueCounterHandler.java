/*
 * Copyright 2013 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.xd.analytics.metrics.core.FieldValueCounterRepository;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.integration.JsonToTupleTransformer;

/**
 * Counts the occurrence of values for a set of JavaBean properties or Tuple fields using a FieldValueCounterRepository.
 * Assumes a String payload is JSON and will convert it to a Tuple.
 *
 * @author Mark Pollack
 * @author David Turanski
 * @author Mark Fisher
 */
public class FieldValueCounterHandler extends AbstractMetricHandler {

	private final FieldValueCounterRepository fieldValueCounterRepository;

	private final String fieldName;

	private final JsonToTupleTransformer jsonToTupleTransformer;

	public FieldValueCounterHandler(FieldValueCounterRepository fieldValueCounterRepository, String nameExpression,
			String fieldName) {
		super(nameExpression);
		Assert.notNull(fieldValueCounterRepository, "FieldValueCounterRepository can not be null");
		Assert.hasText(fieldName, "Field name can not be null or empty string");
		this.fieldValueCounterRepository = fieldValueCounterRepository;
		this.fieldName = fieldName;
		this.jsonToTupleTransformer = new JsonToTupleTransformer();

	}

	@ServiceActivator
	public Message<?> process(Message<?> message) {
		Object payload = message.getPayload();
		if (payload instanceof String) {
			try {
				payload = jsonToTupleTransformer.transformPayload(payload.toString());
			}
			catch (Exception e) {
				throw new MessageTransformationException(message, e.getMessage(), e);
			}
		}
		if (payload instanceof Tuple) {
			processTuple(computeMetricName(message), (Tuple) payload);
		}
		else {
			processPojo(computeMetricName(message), payload);
		}
		return message;
	}

	private void processPojo(String counterName, Object payload) {
		BeanWrapper beanWrapper = new BeanWrapperImpl(payload);
		if (beanWrapper.isReadableProperty(fieldName)) {
			Object value = beanWrapper.getPropertyValue(fieldName);
			processValue(counterName, value);
		}
	}

	private void processTuple(String counterName, Tuple tuple) {
		String[] path = StringUtils.tokenizeToStringArray(fieldName, ".");
		processValueForCounter(counterName, tuple, path);
	}

	private void processValueForCounter(String counterName, Object value, String[] path) {
		String key = path[0];
		Object result = null;
		if (value instanceof List) {
			for (Object item : (List<?>) value) {
				processValueForCounter(counterName, item, path);
			}
		}
		else if (value instanceof Tuple) {
			Tuple t = (Tuple) value;
			if (t.hasFieldName(key)) {
				result = t.getValue(key);
			}
		}
		else if (value instanceof Map) {
			result = ((Map<?, ?>) value).get(key);
		}
		if (result != null) {
			if (path.length == 1) {
				processValue(counterName, result);
			}
			else {
				path = Arrays.copyOfRange(path, 1, path.length);
				processValueForCounter(counterName, result, path);
			}
		}
	}

	protected void processValue(String counterName, Object value) {
		if ((value instanceof Collection) || ObjectUtils.isArray(value)) {
			Collection<?> c = (value instanceof Collection) ? (Collection<?>) value
					: Arrays.asList(ObjectUtils.toObjectArray(value));
			for (Object val : c) {
				// TODO better conversion to a string
				fieldValueCounterRepository.increment(counterName, val.toString());
			}
		}
		else {
			fieldValueCounterRepository.increment(counterName, value.toString());
		}
	}
}
