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

package org.springframework.xd.module.options.mixins;

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Base class for mixins that add the {@code mappedRequestHeaders} option.
 * Implemented as a base abstract class (with some concrete implementations as static inner classes)
 * because the default value for the option varies by adapter.
 * 
 *  @author Eric Bottard
 */
public abstract class MappedRequestHeadersMixin {

	private String mappedRequestHeaders;

	protected MappedRequestHeadersMixin(String mappedRequestHeaders) {
		this.mappedRequestHeaders = mappedRequestHeaders;
	}

	@ModuleOption("request message header names to be propagated to/from the adpater/gateway")
	public void setMappedRequestHeaders(String mappedRequestHeaders) {
		this.mappedRequestHeaders = mappedRequestHeaders;
	}

	public String getMappedRequestHeaders() {
		return mappedRequestHeaders;
	}

	public static class Http extends MappedRequestHeadersMixin {

		public Http() {
			super("HTTP_REQUEST_HEADERS");
		}

	}

	public static class Amqp extends MappedRequestHeadersMixin {

		public Amqp() {
			super("STANDARD_REQUEST_HEADERS");
		}
	}
}
