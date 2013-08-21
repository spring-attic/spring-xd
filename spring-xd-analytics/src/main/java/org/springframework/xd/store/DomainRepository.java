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

import org.springframework.data.repository.PagingAndSortingRepository;


/**
 * Implemented by XD repositories, exists mainly as a mean to capture several interfaces into one.
 * 
 * @author Eric Bottard
 */
public interface DomainRepository<T, ID extends Serializable & Comparable<ID>> extends
		PagingAndSortingRepository<T, ID>, RangeCapableRepository<T, ID> {

}
