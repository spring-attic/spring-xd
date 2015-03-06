/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.xd.dirt.server;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.util.Assert;


/**
 * 
 * @author David Turanski
 */
public abstract class ApplicationUtils {

	public static ApplicationListener<?>[] mergeApplicationListeners(ApplicationListener<?> applicationListener,
			ApplicationListener<?>[] applicationListeners) {
		Assert.notEmpty(applicationListeners, "applicationListeners[] must contain at least one item");
		int newLength = applicationListeners.length + 1;
		ApplicationListener<?>[] mergedApplicationListeners = Arrays.copyOf(applicationListeners,
				newLength);
		mergedApplicationListeners[newLength - 1] = applicationListener;

		return mergedApplicationListeners;
	}

	private static Map<String, Object> removeParentBeans(Map<String, Object> parentBeans, Map<String, Object> beans) {
		for (String key : parentBeans.keySet()) {
			beans.remove(key);
		}
		return beans;
	}

	public static String displayBeans(Map<String, Object> beans, String contextName) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n").append(contextName).append(":\n");
		for (Entry<String, Object> entry : beans.entrySet()) {
			sb.append("\t[").append(entry.getKey()).append("] =")
					.append((entry.getValue() != null ? entry.getValue().getClass().getName() : "null"))
					.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Dump container context configuration details to stdout
	 * 
	 * @param containerContext
	 */
	public static void dumpContainerApplicationContextConfiguration(ApplicationContext containerContext) {
		Map<String, Object> containerBeans;
		Map<String, Object> pluginBeans;
		Map<String, Object> globalBeans;
		Map<String, Object> sharedServerBeans;

		globalBeans = containerContext.getParent().getParent().getParent().getBeansOfType(Object.class);
		sharedServerBeans = containerContext.getParent().getParent().getBeansOfType(Object.class);
		pluginBeans = containerContext.getParent().getBeansOfType(Object.class);
		containerBeans = containerContext.getBeansOfType(Object.class);

		removeParentBeans(pluginBeans, containerBeans);

		removeParentBeans(sharedServerBeans, pluginBeans);

		removeParentBeans(globalBeans, sharedServerBeans);

		System.out.println(displayBeans(globalBeans, "global context"));
		System.out.println(displayBeans(sharedServerBeans, "shared server context"));
		System.out.println(displayBeans(pluginBeans, "plugin context"));
		System.out.println(displayBeans(containerBeans, "container context"));

	}
}
