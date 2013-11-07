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

package org.springframework.xd.dirt.post;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;


/**
 * @author Dave Syer
 */
public class HandlerMappingRegistrar implements DisposableBean,
		InitializingBean {

	private DelegatingHandlerMapping mapping;

	private String path;

	private Object handler;

	public HandlerMappingRegistrar(DelegatingHandlerMapping mapping, String path, Object handler) {
		this.mapping = mapping;
		this.path = path;
		this.handler = handler;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		mapping.register(handler, path);
	}

	@Override
	public void destroy() throws Exception {
		mapping.unregister(path);
	}

}
