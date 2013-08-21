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

import java.math.BigDecimal;

import org.joda.time.DateTime;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.convert.ConversionException;
import org.springframework.util.ClassUtils;
import org.springframework.xd.tuple.DefaultTuple;
import org.springframework.xd.tuple.JsonBytesToTupleConverter;
import org.springframework.xd.tuple.Tuple;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * A class that maps objects to JSON. The result includes type information used to recreate the original object. If type
 * information is not included, JSON will be unmarshalled as a {@link DefaultTuple}.
 * 
 * Custom types must either be compatible with {@link ObjectMapper} or include the appropriate Jackson annotations
 * 
 * @author David Turanski
 * @author Gary Russell
 * 
 */
public class TypedJsonMapper implements BeanClassLoaderAware {

	private final ObjectMapper mapper = new ObjectMapper();

	/*
	 * separate mapper required as a work around for https://github.com/FasterXML/jackson-databind/issues/88
	 */
	private final ObjectMapper unmarshallingMapper = new ObjectMapper();

	private final JsonBytesToTupleConverter jsonBytesToTupleConverter = new JsonBytesToTupleConverter();

	private volatile ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	public TypedJsonMapper() {
		// include type information
		this.mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
		mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
		mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
		unmarshallingMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
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
			Class<?> type = getWrappedType(root);
			if (type == null) {
				return jsonBytesToTupleConverter.convert(bytes);
			}

			String typeName = root.fieldNames().next();
			JsonNode value = root.get(typeName);
			root.get(typeName);

			if (String.class.equals(type)) {
				return value.asText();
			}
			else if (Long.class.equals(type)) {
				return value.asLong();
			}
			else if (Integer.class.equals(type)) {
				return value.asInt();
			}
			else if (BigDecimal.class.equals(type)) {
				return value.decimalValue();
			}
			else if (Double.class.equals(type)) {
				return value.doubleValue();
			}
			else if (Float.class.equals(type)) {
				return value.doubleValue();
			}
			else if (Boolean.class.equals(type)) {
				return value.asBoolean();
			}
			else if (byte[].class.equals(type)) {
				return value.binaryValue();
			}
			else if (char[].class.equals(type)) {
				return value.asText().toCharArray();
			}
			else if (DateTime.class.equals(type)) {
				return new DateTime(value.get("millis").asLong());
			}
			else if (DefaultTuple.class.equals(type)) {
				return jsonBytesToTupleConverter.convert(value.binaryValue());
			}
			return mapper.treeToValue(value, type);

		}
		catch (Exception e) {
			throw new SmartJsonConversionException(e.getMessage(), e);
		}
	}

	/**
	 * UnMarshall to a requested class
	 * 
	 * @param bytes
	 * @param className
	 * @return the object
	 */
	public Object fromBytes(byte[] bytes, String className) {
		Object obj = null;
		try {
			JsonNode root = unmarshallingMapper.readTree(bytes);
			JsonNode value = root;
			Class<?> wrappedType = getWrappedType(root);
			if (wrappedType != null) {
				String typeName = root.fieldNames().next();
				value = root.get(typeName);
			}

			Class<?> type = this.beanClassLoader.loadClass(className);
			if (Tuple.class.isAssignableFrom(type)) {
				byte[] from = bytes;
				if (String.class.equals(wrappedType)) {
					from = value.asText().getBytes();
				}
				else if (wrappedType != null) {
					from = value.toString().getBytes();
				}
				return jsonBytesToTupleConverter.convert(from);
			}
			obj = mapper.treeToValue(value, type);
		}
		catch (Exception e) {
			throw new SmartJsonConversionException(e.getMessage(), e);
		}
		return obj;
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

	/*
	 * Determine if serialized content is wrapped with type information. In the case of a raw JSON string, should return
	 * null.
	 */
	private Class<?> getWrappedType(JsonNode root) {
		String typeName = root.fieldNames().next();
		JsonNode value = root.get(typeName);
		/*
		 * Collections, java.util.Date are serialized as arrays
		 */
		if (value.isArray()) {
			String className = value.get(0).asText();
			try {
				return this.beanClassLoader.loadClass(className);
			}
			catch (ClassNotFoundException e) {
				// In this case, interpret as a raw JSON string containing an array
				return null;
			}
		}
		if (typeName.equals("String")) {
			return String.class;
		}
		else if (typeName.equals("Long")) {
			return Long.class;
		}
		else if (typeName.equals("Integer")) {
			return Integer.class;
		}
		else if (typeName.equals("BigDecimal")) {
			return BigDecimal.class;
		}
		else if (typeName.equals("Double")) {
			return Double.class;
		}
		else if (typeName.equals("Float")) {
			return Float.class;
		}
		else if (typeName.equals("Boolean")) {
			return Boolean.class;
		}
		else if (typeName.equals("byte[]")) {
			return byte[].class;
		}
		else if (typeName.equals("char[]")) {
			return char[].class;
		}
		else if (typeName.equals("DateTime")) {
			return DateTime.class;
		}
		String className = value.get("@class") == null ? null : value.get("@class").asText();
		if (className != null) {
			try {
				return this.beanClassLoader.loadClass(className);
			}
			catch (ClassNotFoundException e) {
				throw new SmartJsonConversionException(e.getMessage(), e);
			}
		}
		return null;
	}
}
