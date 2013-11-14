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

package org.springframework.xd.dirt.module.support;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.ApplicationContext;


/**
 * An advice that will set the {@link Thread#setContextClassLoader(ClassLoader) thread context ClassLoader} to the
 * enclosing {@link ApplicationContext#getClassLoader() ApplicationContext's class loader} before invocation and reset
 * it afterwards.
 * 
 * @author Eric Bottard
 */
public class ThreadContextClassLoaderSetterAdvice implements MethodInterceptor, BeanClassLoaderAware {

	private ClassLoader classLoaderToUse;

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoaderToUse);
			return invocation.proceed();
		}
		finally {
			Thread.currentThread().setContextClassLoader(previous);
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoaderToUse = classLoader;
	}

}
