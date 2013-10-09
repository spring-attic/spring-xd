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

package org.springframework.xd.dirt.container.store;

import org.springframework.data.redis.core.RedisOperations;
import org.springframework.xd.store.AbstractRedisRepository;


/**
 * Redis specific implementation for the runtime container info repository.
 * 
 * @author Ilayaperumal Gopinathan
 */
public class RedisRuntimeContainerInfoRepository extends AbstractRedisRepository<RuntimeContainerInfoEntity, String>
		implements RuntimeContainerInfoRepository {

	private static final int CONTAINER_ID_INDEX = 0;

	private static final int JVM_NAME_INDEX = 1;

	private static final int HOST_NAME_INDEX = 2;

	private static final int IP_ADDRESS_INDEX = 3;

	public RedisRuntimeContainerInfoRepository(RedisOperations<String, String> redisOperations) {
		super("containers", redisOperations);
	}

	@Override
	protected RuntimeContainerInfoEntity deserialize(String redisKey, String value) {
		String[] parts = value.split("\n");
		return new RuntimeContainerInfoEntity(parts[CONTAINER_ID_INDEX], parts[JVM_NAME_INDEX], parts[HOST_NAME_INDEX],
				parts[IP_ADDRESS_INDEX]);
	}

	@Override
	protected String serialize(RuntimeContainerInfoEntity container) {
		return container.getId() + "\n" + container.getJvmName() + "\n" + container.getHostName() + "\n"
				+ container.getIpAddress();
	}

	@Override
	protected String keyFor(RuntimeContainerInfoEntity container) {
		return container.getId();
	}

	@Override
	protected String serializeId(String id) {
		return id;
	}

	@Override
	protected String deserializeId(String string) {
		return string;
	}

}
