/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.x.json;

import org.joda.time.DateTime;

import org.springframework.core.convert.ConversionException;
import org.springframework.xd.tuple.DefaultTuple;
import org.springframework.xd.tuple.JsonBytesToTupleConverter;
import org.springframework.xd.tuple.Tuple;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * A class that maps objects to JSON. The result includes type information used
 * to recreate the original object. If type information is not included, JSON
 * will be unmarshalled as a {@link DefaultTuple}.
 *
 * Custom types must either be compatible with {@link ObjectMapper} or include the appropriate Jackson
 * annotations
 *
 * @author David Turanski
 * @author Gary Russell
 *
 */
public class TypedJsonMapper {

	private final ObjectMapper mapper = new ObjectMapper();
	/*
	 * separate mapper required as a work around for
	 * https://github.com/FasterXML/jackson-databind/issues/88
	 */
	private final ObjectMapper unmarshallingMapper = new ObjectMapper();

	private final JsonBytesToTupleConverter jsonBytesToTupleConverter = new JsonBytesToTupleConverter();

	public TypedJsonMapper() {
		//include type information
		this.mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
		mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
	}

	public byte[] toBytes(Object t) throws ConversionException {
		if (t == null) {
			return new byte[0];
		}

		if (t instanceof Tuple) {
			return t.toString().getBytes();
		}

		try {
			return mapper.writeValueAsBytes(t);
		}
		catch (Exception e) {
			throw new SmartJsonConversionException(e.getMessage(), e);
		}
	}

	public Object fromBytes(byte[] bytes) throws ConversionException {
		if (bytes == null || bytes.length == 0) {
			return null;
		}
		try {
			JsonNode root = unmarshallingMapper.readTree(bytes);
			String typeName = root.fieldNames().next();
			JsonNode value = root.get(typeName);
			/*
			 * Collections, java.util.Date are serialized as arrays
			 */
			if (value.isArray()) {
				String className = value.get(0).asText();
				return mapper.treeToValue(value,Class.forName(className));
			}
			if (typeName.equals("String")) {
				return value.asText();
			}
			else if (typeName.equals("Long")) {
				return value.asLong();
			}
			else if (typeName.equals("Integer")) {
				return value.asInt();
			}
			else if (typeName.equals("BigDecimal")) {
				return value.decimalValue();
			}
			else if (typeName.equals("Double")) {
				return value.doubleValue();
			}
			else if (typeName.equals("Float")) {
				return value.doubleValue();
			}
			else if (typeName.equals("Boolean")) {
				return value.asBoolean();
			}
			else if (typeName.equals("byte[]")) {
				return value.binaryValue();
			}
			else if (typeName.equals("char[]")) {
				return value.asText().toCharArray();
			}
			else if (typeName.equals("DateTime")) {
				return new DateTime(value.get("millis").asLong());
			}
			else if (typeName.equals("DefaultTuple")) {
				return jsonBytesToTupleConverter.convert(value.binaryValue());
			}
			String className = value.get("@class") == null ? null : value.get("@class").asText();
			if (className != null) {
				return mapper.treeToValue(value, Class.forName(className));
			}
			return jsonBytesToTupleConverter.convert(bytes);
		}
		catch (Exception e) {
			throw new SmartJsonConversionException(e.getMessage(), e);
		}
	}

	@SuppressWarnings("serial")
	public static class SmartJsonConversionException extends ConversionException {

		public SmartJsonConversionException(String message, Throwable cause) {
			super(message, cause);
		}

		public SmartJsonConversionException(String message) {
			super(message);
		}

	}
}
