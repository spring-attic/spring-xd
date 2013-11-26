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
import org.springframework.xd.dirt.module.ModuleDependencyRepository;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamDefinitionRepositoryUtils;
import org.springframework.xd.module.ModuleDefinition;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * An implementation of {@link StreamDefinitionRepository} that persists StreamDefinition in Redis.
 * 
 * @author Eric Bottard
 */
public class RedisStreamDefinitionRepository extends AbstractRedisDefinitionRepository<StreamDefinition> implements
		StreamDefinitionRepository {

	private ObjectMapper objectMapper = new ObjectMapper();

	private final ModuleDependencyRepository dependencyRepository;

	public RedisStreamDefinitionRepository(RedisOperations<String, String> redisOperations,
			ModuleDependencyRepository dependencyRepository) {
		super("stream.definitions", redisOperations);
		this.dependencyRepository = dependencyRepository;
		objectMapper.addMixInAnnotations(ModuleDefinition.class, ModuleDefinitionMixin.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public StreamDefinition save(StreamDefinition entity) {
		StreamDefinition sd = super.save(entity);
		StreamDefinitionRepositoryUtils.saveDependencies(dependencyRepository, sd);
		return sd;
	}

	@Override
	public void delete(StreamDefinition entity) {
		StreamDefinitionRepositoryUtils.deleteDependencies(dependencyRepository, entity);
		super.delete(entity);
	}

	@Override
	public void delete(String id) {
		StreamDefinition def = this.findOne(id);
		if (def != null) {
			this.delete(def);
		}
	}

	@Override
	protected StreamDefinition deserialize(String redisKey, String v) {
		try {
			return this.objectMapper.readValue(v, StreamDefinition.class);
		}
		catch (Exception ex) {
			throw new SerializationException("Could not read JSON: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected String serialize(StreamDefinition entity) {
		try {
			return this.objectMapper.writeValueAsString(entity);
		}
		catch (Exception ex) {
			throw new SerializationException("Could not write JSON: " + ex.getMessage(), ex);
		}
	}

}
