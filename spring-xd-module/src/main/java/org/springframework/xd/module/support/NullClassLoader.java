/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.module.support;

/**
 * A ClassLoader that can't find or load anything. Useful for insulating a (parent last) child ClassLoader from the main runtime
 * classpath, in cases where it is expected that a class or resource will be resolved by the child ClassLoader. In effect,
 * using an instance of this class as a parent ClassLoader is equivalent to using no-parent, as it would always fail.
 *
 * @author Eric Bottard
 */
public class NullClassLoader extends ClassLoader {

	public static final ClassLoader NO_PARENT = new NullClassLoader();

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		throw new ClassNotFoundException();
	}
}
