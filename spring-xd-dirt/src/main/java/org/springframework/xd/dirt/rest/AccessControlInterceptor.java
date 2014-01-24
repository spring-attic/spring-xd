/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * An interceptor to handle basic CORS logic.
 * 
 * @author Eric Bottard
 */
class AccessControlInterceptor extends HandlerInterceptorAdapter {

	private static final String LOCATION = "Location";

	private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

	private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

	private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

	private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

	private static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";

	private static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";

	private static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

	private static final String CACHE_SECONDS = "300";

	private final String allowedOrigin;

	AccessControlInterceptor(String allowedOrigin) {
		Assert.hasText(allowedOrigin);
		this.allowedOrigin = allowedOrigin;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		// For PUT requests we need an extra round-trip
		// See e.g. http://www.html5rocks.com/en/tutorials/cors/

		String acRequestMethod = request.getHeader(ACCESS_CONTROL_REQUEST_METHOD);
		String acRequestHeaders = request.getHeader(ACCESS_CONTROL_REQUEST_HEADERS);

		response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);

		if (HttpMethod.OPTIONS.toString().equals(request.getMethod()) && hasValue(acRequestMethod)) {
			// this is a preflight check our API only needs this for PUT
			// requests, anything we can PUT we can also GET
			response.addHeader(ACCESS_CONTROL_ALLOW_METHODS, HttpMethod.GET.toString());
			response.addHeader(ACCESS_CONTROL_ALLOW_METHODS, HttpMethod.POST.toString());
			response.addHeader(ACCESS_CONTROL_ALLOW_METHODS, HttpMethod.PUT.toString());
			response.addHeader(ACCESS_CONTROL_ALLOW_METHODS, HttpMethod.DELETE.toString());
			response.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, acRequestHeaders);
			response.setHeader(ACCESS_CONTROL_MAX_AGE, CACHE_SECONDS);

			return false; // Don't continue processing, return to browser immediately
		}
		else {
			response.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS, LOCATION);
		}

		return true; // Not a preflight check, continue as normal
	}

	private boolean hasValue(String s) {
		return ((s != null) && !s.isEmpty());
	}
}
