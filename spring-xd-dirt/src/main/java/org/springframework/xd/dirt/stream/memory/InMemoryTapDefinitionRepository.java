/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.dirt.stream.memory;

import org.springframework.xd.dirt.stream.TapDefinition;
import org.springframework.xd.dirt.stream.TapDefinitionRepository;
import org.springframework.xd.store.AbstractInMemoryRepository;

/**
 * @author David Turanski
 * 
 */
public class InMemoryTapDefinitionRepository extends AbstractInMemoryRepository<TapDefinition, String> implements
		TapDefinitionRepository {

	@Override
	protected String keyFor(TapDefinition entity) {
		return entity.getName();
	}

}
