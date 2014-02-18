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

package org.springframework.integration.x.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.util.Assert;


/**
 * Adapts from Netty's {@link HttpRequest} to Spring Integration's {@link HttpInputMessage}.
 * 
 * @author Eric Bottard
 */
public class NettyHttpInputMessage implements HttpInputMessage {

	private final HttpRequest httpRequest;

	/**
	 * @param request
	 */
	public NettyHttpInputMessage(HttpRequest request) {
		Assert.notNull(request, "request cannot be null");
		httpRequest = request;
	}

	@Override
	public HttpHeaders getHeaders() {
		HttpHeaders result = new HttpHeaders();
		for (Map.Entry<String, String> e : httpRequest.getHeaders()) {
			result.add(e.getKey(), e.getValue());
		}
		return result;
	}

	@Override
	public InputStream getBody() throws IOException {
		return new ChannelBufferInputStream(httpRequest.getContent());
	}
}
