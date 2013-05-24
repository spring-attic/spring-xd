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
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.xd.analytics.metrics.core.FieldValueCounterService;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.integration.JsonToTupleTransformer;

/**
 * Counts the occurrence of values for a set of JavaBean properties or Tuple fields using a FieldValueCounterService.
 * Assumes a String payload is JSON and will convert it to a Tuple.
 * 
 * 
 * @author Mark Pollack
 * @author David Turanski
 *
 */
public class FieldValueCounterHandler {

	private final FieldValueCounterService fieldValueCounterService;

	private final Map<String, String> fieldNameToCounterNameMap;

	private final JsonToTupleTransformer jsonToTupleTransformer;

	public FieldValueCounterHandler(FieldValueCounterService fieldValueCounterService, String counterName,
			String fieldName) {
		Assert.notNull(fieldValueCounterService, "FieldValueCounterService can not be null");
		Assert.notNull(counterName, "counter Name can not be null");
		Assert.hasText(fieldName, "Field name can not be null or empty string");
		this.fieldValueCounterService = fieldValueCounterService;
		this.fieldNameToCounterNameMap = new HashMap<String, String>();
		fieldNameToCounterNameMap.put(fieldName, counterName);
		this.jsonToTupleTransformer = new JsonToTupleTransformer();

	}

	@ServiceActivator
	public Message<?> process(Message<?> message) {
		Object payload = message.getPayload();
		if (payload instanceof String) {
			try {
				payload = jsonToTupleTransformer.transformPayload(payload.toString());
			} catch (Exception e) {
				throw new MessageTransformationException(message, e);
			}
		}
		if (payload instanceof Tuple) {
			processTuple((Tuple) payload);
		} else {
			processPojo(payload);
		}
		return message;
	}

	private void processPojo(Object payload) {
		BeanWrapper beanWrapper = new BeanWrapperImpl(payload);
		for (Map.Entry<String, String> entry : this.fieldNameToCounterNameMap.entrySet()) {
			String fieldName = entry.getKey();
			String counterName = entry.getValue();
			if (beanWrapper.isReadableProperty(fieldName)) {
				Object value = beanWrapper.getPropertyValue(fieldName);
				processValue(counterName, value);
			}
		}
	}

	private void processTuple(Tuple tuple) {
		for (Map.Entry<String, String> entry : this.fieldNameToCounterNameMap.entrySet()) {
			String fieldName = entry.getKey();
			String counterName = entry.getValue();
			if (tuple.hasFieldName(fieldName)) {
				Object value = tuple.getValue(fieldName);
				processValue(counterName, value);
			}
		}
	}

	protected void processValue(String counterName, Object value) {
		if ((value instanceof Collection) || ObjectUtils.isArray(value)) {
			Collection<?> c = (value instanceof Collection) ? (Collection<?>) value : Arrays.asList(ObjectUtils
					.toObjectArray(value));
			for (Object val : c) {
				//TODO better conversion to a string
				fieldValueCounterService.increment(counterName, val.toString());
			}
		} else {
			fieldValueCounterService.increment(counterName, value.toString());
		}
	}
}
