/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.xd.store;

import java.io.Serializable;

/**
 * To be implemented by repositories that know how to return a slice of their data.
 * 
 * @author Eric Bottard
 */
public interface RangeCapableRepository<T, ID extends Serializable & Comparable<ID>> {

	/**
	 * Return entities whose ids range between {@code from} and {@code to}. Note that it is possible that entities with
	 * those exact ids do not exist. If they do exist, the two boolean parameters control whether to include those
	 * results or not. A query from {@code x} to {@code x} returns an empty result, unless both {@code fromInclusive}
	 * and {@code toInclusive} are true.
	 */
	public Iterable<T> findAllInRange(ID from, boolean fromInclusive, ID to, boolean toInclusive);

}
