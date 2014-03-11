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

import java.util.Arrays;

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

}
