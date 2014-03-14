/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.stream.redis;

import org.springframework.data.redis.core.RedisOperations;
import org.springframework.xd.dirt.stream.Job;
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xd.dirt.stream.JobRepository;

/**
 * Redis implementation of {@link JobRepository}, uses a {@link RedisJobDefinitionRepository} in turn.
 * 
 * @author Eric Bottard
 */
public class RedisJobRepository extends AbstractRedisInstanceRepository<Job> implements JobRepository {

	private final JobDefinitionRepository jobDefinitionRepository;

	public RedisJobRepository(RedisOperations<String, String> redisOperations,
			JobDefinitionRepository jobDefinitionRepository) {
		super("jobs", redisOperations);
		this.jobDefinitionRepository = jobDefinitionRepository;
	}

	@Override
	protected Job deserialize(String id, String v) {
		// TODO: add other parts that represent runtime properties
		String[] parts = v.split("\n");
		JobDefinition def = jobDefinitionRepository.findOne(parts[0]);
		Job job = new Job(def);
		return job;
	}

	@Override
	protected String serialize(Job entity) {
		return entity.getDefinition().getName();
	}

}
