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

package org.springframework.xd.dirt.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.util.Assert;


/**
 * 
 * @author David Turanski
 */
abstract class ApplicationUtils {

	static ApplicationListener<?>[] mergeApplicationListeners(ApplicationListener<?> applicationListener,
			ApplicationListener<?>[] applicationListeners) {
		Assert.notEmpty(applicationListeners, "applicationListeners[] must contain at least one item");
		int newLength = applicationListeners.length + 1;
		ApplicationListener<?>[] mergedApplicationListeners = Arrays.copyOf(applicationListeners,
				newLength);
		mergedApplicationListeners[newLength - 1] = applicationListener;

		return mergedApplicationListeners;
	}

	static void dumpContainerApplicationContextConfiguration(ApplicationContext containerContext) {
		Map<String, Object> containerBeans = new HashMap<String, Object>();
		Map<String, Object> pluginBeans = new HashMap<String, Object>();
		Map<String, Object> globalBeans = new HashMap<String, Object>();

		globalBeans = containerContext.getParent().getParent().getBeansOfType(Object.class);
		pluginBeans = containerContext.getParent().getBeansOfType(Object.class);
		containerBeans = containerContext.getBeansOfType(Object.class);

		List<String> dups;

		dups = new ArrayList<String>();

		for (Entry<String, Object> entry : containerBeans.entrySet()) {
			if (pluginBeans.containsKey(entry.getKey())) {
				dups.add(entry.getKey());
			}
		}

		System.out.println("core: found " + dups.size() + dups);
		for (String key : dups) {
			containerBeans.remove(key);
		}


		dups = new ArrayList<String>();
		for (Entry<String, Object> entry : pluginBeans.entrySet()) {
			if (globalBeans.containsKey(entry.getKey())) {
				dups.add(entry.getKey());
			}
		}
		System.out.println("container: found " + dups.size() + dups);
		for (String key : dups) {
			pluginBeans.remove(key);
		}

		System.out.println("global context:");
		for (Entry<String, Object> entry : globalBeans.entrySet()) {
			System.out.println("\t" + entry.getKey() + "=" + entry.getValue().getClass().getName());
		}
		System.out.println();
		System.out.println("plugin context:");
		for (Entry<String, Object> entry : pluginBeans.entrySet()) {
			System.out.println("\t" + entry.getKey() + "=" + entry.getValue().getClass().getName());
		}
		System.out.println();
		System.out.println("container context:");
		for (Entry<String, Object> entry : containerBeans.entrySet()) {
			System.out.println("\t" + entry.getKey() + "=" + entry.getValue().getClass().getName());
		}
	}

}
