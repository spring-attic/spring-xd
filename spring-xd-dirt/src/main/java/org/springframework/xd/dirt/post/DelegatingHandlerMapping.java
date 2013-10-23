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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeansException;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;


/**
 * @author Dave Syer
 */
public class DelegatingHandlerMapping extends AbstractUrlHandlerMapping {

	private Map<String, Object> map = new HashMap<String, Object>();

	public DelegatingHandlerMapping() {
		setOrder(Ordered.HIGHEST_PRECEDENCE);
	}

	public void register(Object bean, String path) throws BeansException {
		map.put(path, bean);
	}

	public void unregister(String path) throws BeansException {
		map.remove(path);
	}

	@Override
	protected Object lookupHandler(String urlPath, HttpServletRequest request) throws Exception {
		return map.get(urlPath);
	}

}
