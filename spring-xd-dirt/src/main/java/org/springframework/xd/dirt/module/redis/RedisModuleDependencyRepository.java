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

package org.springframework.xd.dirt.module.redis;

import java.util.Set;

import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.xd.dirt.module.ModuleDependencyRepository;
import org.springframework.xd.module.ModuleType;


/**
 * Redis implementation of {@link ModuleDependencyRepository}. Uses sets whose name is of the form
 * {@code dependencies.module.<type>:<name>}.
 * 
 * @author Eric Bottard
 */
public class RedisModuleDependencyRepository implements ModuleDependencyRepository {

	private RedisOperations<String, String> redisOperations;

	public RedisModuleDependencyRepository(RedisOperations<String, String> redisOperations) {
		this.redisOperations = redisOperations;
	}


	@Override
	public void store(String moduleName, ModuleType type, String target) {
		setOpsFor(moduleName, type).add(target);
	}

	@Override
	public void delete(String module, ModuleType type, String target) {
		setOpsFor(module, type).remove(target);
	}

	@Override
	public Set<String> find(String name, ModuleType type) {
		return setOpsFor(name, type).members();
	}

	private BoundSetOperations<String, String> setOpsFor(String moduleName, ModuleType type) {
		return redisOperations.boundSetOps(String.format("dependencies.module.%s:%s", type.name(), moduleName));
	}

}
