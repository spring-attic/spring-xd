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

package org.springframework.xd.dirt.zookeeper;


/**
 * Utility methods for ZooKeeper.
 * @author David Turanski
 */
public abstract class ZooKeeperUtils {

	/**
	 * Utility method to wrap a Throwable in a {@link ZooKeeperAccessException}.
	 * @param t the throwable
	 */
	public static RuntimeException wrapThrowable(Throwable t) {
		return wrapThrowable(t, null);
	}

	/**
	 * Utility method to wrap a Throwable in a {@link ZooKeeperAccessException}.
	 * @param t the throwable
	 * @param message use this message if not null
	 */
	public static RuntimeException wrapThrowable(Throwable t, String message) {
		if (message != null) {
			return new ZooKeeperAccessException(message, t);
		}
		if (t instanceof RuntimeException) {
			return (RuntimeException) t;
		}
		else {
			return new ZooKeeperAccessException(t.getMessage(), t);
		}
	}

	/**
	 * Throw a wrapped exception ignoring specific Exception types, if any.
	 * @param t the throwable
	 * @param ignored a varargs list of ignored exception types
	 */
	@SuppressWarnings("unchecked")
	public static void wrapAndThrowIgnoring(Throwable t, Class... ignored) {
		if (ignored != null) {
			for (Class<? extends Exception> e : ignored) {
				if (e.isAssignableFrom(t.getClass())) {
					return;
				}
			}
		}
		throw wrapThrowable(t);
	}
}
