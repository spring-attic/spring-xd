/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.stream.redis;

import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xd.module.ModuleDefinition;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * An implementation of {@link JobDefinitionRepository} that persists @{link JobDefinition} in Redis.
 * 
 * @author Glenn Renfro
 */
public class RedisJobDefinitionRepository extends AbstractRedisDefinitionRepository<JobDefinition> implements
		JobDefinitionRepository {

	private ObjectMapper objectMapper = new ObjectMapper();

	public RedisJobDefinitionRepository(RedisOperations<String, String> redisOperations) {
		super("job.definitions", redisOperations);
		objectMapper.addMixInAnnotations(ModuleDefinition.class, ModuleDefinitionMixin.class);
	}

	@Override
	protected JobDefinition deserialize(String redisKey, String v) {
		try {
			return this.objectMapper.readValue(v, JobDefinition.class);
		}
		catch (Exception ex) {
			throw new SerializationException("Could not read JSON: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected String serialize(JobDefinition entity) {
		try {
			return this.objectMapper.writeValueAsString(entity);
		}
		catch (Exception ex) {
			throw new SerializationException("Could not write JSON: " + ex.getMessage(), ex);
		}
	}

}
