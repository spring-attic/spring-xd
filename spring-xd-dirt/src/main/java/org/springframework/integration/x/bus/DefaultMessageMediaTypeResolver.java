/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.x.bus;


import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;

/**
 * @author Rossen Stoyanchev
 * @author David Turanski
 * @since 1.0
 */
public class DefaultMessageMediaTypeResolver implements MessageMediaTypeResolver {


	@Override
	public MediaType resolveMediaType(Message<?> message) {

		Object value = message.getHeaders().get(MessageHeaders.CONTENT_TYPE);
		if (value == null) {
			return null;
		}

		if (value instanceof MediaType) {
			return (MediaType) value;
		}
		else if (value instanceof String) {
			return MediaType.valueOf((String) value);
		}
		else {
			throw new InvalidMediaTypeException(value.toString(),
					"Unexpected contentType header value type " + value.getClass());
		}
	}
}
