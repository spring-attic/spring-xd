/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.xd.dirt.integration.bus.converter;

import static org.springframework.util.MimeType.valueOf;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON;
import static org.springframework.util.MimeTypeUtils.APPLICATION_OCTET_STREAM;

import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.xd.tuple.DefaultTuple;


/**
 * Message conversion utility methods.
 *
 * @author David Turanski
 * @author Gary Russell
 */
public class MessageConverterUtils {

	/**
	 * An XD MimeType specifying a {@link org.springframework.xd.tuple.Tuple}.
	 */
	public static final MimeType X_XD_TUPLE = MimeType.valueOf("application/x-xd-tuple");

	/**
	 * An XD MimeType for specifying a String.
	 */
	public static final MimeType X_XD_STRING = MimeType.valueOf("application/x-xd-string");

	/**
	 * A general MimeType for Java Types.
	 */
	public static final MimeType X_JAVA_OBJECT = MimeType.valueOf("application/x-java-object");

	/**
	 * A general MimeType for a Java serialized byte array.
	 */
	public static final MimeType X_JAVA_SERIALIZED_OBJECT = MimeType.valueOf("application/x-java-serialized-object");

	/**
	 * Map the contentType to a target class.
	 *
	 * @param contentType the content type
	 * @param classLoader the class loader used to resolve the class
	 * @return the class for the content type
	 */
	public static Class<?> getJavaTypeForContentType(MimeType contentType, ClassLoader classLoader) {
		if (X_JAVA_OBJECT.includes(contentType)) {
			if (contentType.getParameter("type") != null) {
				try {
					return ClassUtils.forName(contentType.getParameter("type"), classLoader);
				}
				catch (Exception e) {
					throw new ConversionException(e.getMessage(), e);
				}
			}
			else {
				return Object.class;
			}
		}
		else if (APPLICATION_JSON.equals(contentType)) {
			return String.class;
		}
		else if (valueOf("text/*").includes(contentType)) {
			return String.class;
		}
		else if (X_XD_TUPLE.includes(contentType)) {
			return DefaultTuple.class;
		}
		else if (APPLICATION_OCTET_STREAM.includes(contentType)) {
			return byte[].class;
		}
		else if (X_JAVA_SERIALIZED_OBJECT.includes(contentType)) {
			return byte[].class;
		}
		else if (X_XD_STRING.includes(contentType)) {
			return String.class;
		}
		return null;
	}

	/**
	 * Build the conventional {@link MimeType} for a java object
	 *
	 * @param clazz the java type
	 * @return the MIME type
	 */
	public static MimeType javaObjectMimeType(Class<?> clazz) {
		return MimeType.valueOf("application/x-java-object;type=" + clazz.getName());
	}
}
