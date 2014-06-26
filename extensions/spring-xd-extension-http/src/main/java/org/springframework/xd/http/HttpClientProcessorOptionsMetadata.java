/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.http;

import static org.springframework.xd.http.HttpClientProcessorOptionsMetadata.HttpMethod.POST;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.springframework.xd.module.options.mixins.MappedRequestHeadersMixin;
import org.springframework.xd.module.options.mixins.MappedResponseHeadersMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Captures options for the {@code http-client} processor module.
 * 
 * @author Eric Bottard
 */
@Mixin({ MappedRequestHeadersMixin.Http.class, MappedResponseHeadersMixin.Http.class })
public class HttpClientProcessorOptionsMetadata {

	private String url;

	private int replyTimeout;

	private HttpMethod httpMethod = POST;

	private String charset = "UTF-8";

	@NotNull
	public String getCharset() {
		return charset;
	}

	@ModuleOption("the charset to use when in the Content-Type header when emitting Strings")
	public void setCharset(String charset) {
		this.charset = charset;
	}

	public static enum HttpMethod {
		OPTIONS, GET, HEAD, POST, PUT, PATCH, DELETE, TRACE, CONNECT;
	}

	@NotNull
	public String getUrl() {
		return url;
	}

	@Min(-1)
	public int getReplyTimeout() {
		return replyTimeout;
	}

	@NotNull
	public HttpMethod getHttpMethod() {
		return httpMethod;
	}

	@ModuleOption("the url to perform an http request on")
	public void setUrl(String url) {
		this.url = url;
	}


	@ModuleOption("the amount of time to wait (ms) for a response from the remote server")
	public void setReplyTimeout(int replyTimeout) {
		this.replyTimeout = replyTimeout;
	}

	@ModuleOption("the http method to use when performing the request")
	public void setHttpMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}


}
