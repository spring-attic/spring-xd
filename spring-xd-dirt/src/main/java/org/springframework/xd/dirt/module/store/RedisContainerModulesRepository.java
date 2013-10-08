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

package org.springframework.xd.dirt.module.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.RedisOperations;


/**
 * Redis specific implementation for runtime modules repository where modules mapped to their containers
 * 
 * @author Ilayaperumal Gopinathan
 */
public class RedisContainerModulesRepository extends AbstractRedisModulesRepository implements
		ContainerModulesRepository {

	public RedisContainerModulesRepository(String repoPrefix, RedisOperations<String, String> redisOperations) {
		super(repoPrefix, redisOperations);
	}

	@Override
	protected String keyForEntity(ModuleEntity entity) {
		return entity.getContainerId();
	}

	@Override
	public List<ModuleEntity> findAll(String containerId) {
		Map<String, String> entityValues = getHashOperations().entries(redisKeyFromId(containerId));
		List<ModuleEntity> result = new ArrayList<ModuleEntity>();
		for (Map.Entry<String, String> entry : entityValues.entrySet()) {
			result.add(deserialize(entry.getKey(), entry.getValue()));
		}
		return result;
	}

}
