/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.integration.bus;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * Encodes requested headers into payload.
 *
 * @author Eric Bottard
 */
public class EmbeddedHeadersMessageConverter {

	/**
	 * Return a new message where some of the original headers of {@code original}
	 * have been embedded into the new message payload.
	 */
	public Message<byte[]> embedHeaders(Message<byte[]> original, String... headers)
			throws UnsupportedEncodingException {
		String[] headerValues = new String[headers.length];
		int n = 0;
		int headerCount = 0;
		int headersLength = 0;
		for (String header : headers) {
			String value = original.getHeaders().get(header) == null ? null
					: original.getHeaders().get(header).toString();
			headerValues[n++] = value;
			if (value != null) {
				headerCount++;
				headersLength += header.length() + value.length();
			}
		}
		byte[] newPayload = new byte[original.getPayload().length + headersLength + headerCount * 2 + 1];
		ByteBuffer byteBuffer = ByteBuffer.wrap(newPayload);
		byteBuffer.put((byte) headerCount);
		for (int i = 0; i < headers.length; i++) {
			if (headerValues[i] != null) {
				byteBuffer.put((byte) headers[i].length());
				byteBuffer.put(headers[i].getBytes("UTF-8"));
				byteBuffer.put((byte) headerValues[i].length());
				byteBuffer.put(headerValues[i].getBytes("UTF-8"));
			}
		}
		byteBuffer.put(original.getPayload());
		return MessageBuilder.withPayload(newPayload).copyHeaders(original.getHeaders()).build();
	}

	/**
	 * Return a message where headers, that were originally embedded into the payload, have been promoted
	 * back to actual headers. The new payload is now the original payload.
	 */
	public Message<byte[]> extractHeaders(Message<byte[]> message) throws UnsupportedEncodingException {
		byte[] bytes = message.getPayload();
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		int headerCount = byteBuffer.get();
		Map<String, Object> headers = new HashMap<String, Object>();
		for (int i = 0; i < headerCount; i++) {
			int len = byteBuffer.get();
			String headerName = new String(bytes, byteBuffer.position(), len, "UTF-8");
			byteBuffer.position(byteBuffer.position() + len);
			len = byteBuffer.get();
			String headerValue = new String(bytes, byteBuffer.position(), len, "UTF-8");
			byteBuffer.position(byteBuffer.position() + len);
			if (IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER.equals(headerName)
					|| IntegrationMessageHeaderAccessor.SEQUENCE_SIZE.equals(headerName)) {
				headers.put(headerName, Integer.parseInt(headerValue));
			}
			else {
				headers.put(headerName, headerValue);
			}
		}
		byte[] newPayload = new byte[byteBuffer.remaining()];
		byteBuffer.get(newPayload);
		return MessageBuilder.withPayload(newPayload).copyHeaders(headers).build();
	}

}
