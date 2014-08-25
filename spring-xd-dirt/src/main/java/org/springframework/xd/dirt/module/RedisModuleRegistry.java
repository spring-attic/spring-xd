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

package org.springframework.xd.dirt.module;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

/**
 * A {@link ModuleRegistry} that stores module definitions in various redis hashes.
 * 
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Glenn Renfro
 */
public class RedisModuleRegistry extends AbstractModuleRegistry {

	private final StringRedisTemplate redisTemplate;

	public RedisModuleRegistry(RedisConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		this.redisTemplate = new StringRedisTemplate(connectionFactory);
	}

	@Override
	protected Resource locateApplicationContext(String name, ModuleType type) {
		Object config = hashOpsForType(type).get(name);
		return (config != null) ? new NamedByteArrayResource(name, config.toString().getBytes()) : null;
	}

	@Override
	public List<ModuleDefinition> findDefinitions(ModuleType type) {
		ArrayList<ModuleDefinition> results = new ArrayList<ModuleDefinition>();
		for (Resource resource : locateApplicationContexts(type)) {
			String name = resource.getFilename().substring(0,
					resource.getFilename().lastIndexOf('.'));
			results.add(new ModuleDefinition(name, type, resource, maybeLocateClasspath(resource, name,
					type)));
		}
		return results;
	}

	@Override
	protected List<Resource> locateApplicationContexts(ModuleType type) {
		ArrayList<Resource> resources = new ArrayList<Resource>();
		BoundHashOperations<String, String, Object> hash = hashOpsForType(type);
		for (String name : hash.keys()) {
			resources.add(new NamedByteArrayResource(name, hash.get(name).toString().getBytes()));
		}
		return resources;
	}

	@Override
	public List<ModuleDefinition> findDefinitions() {
		ArrayList<ModuleDefinition> results = new ArrayList<ModuleDefinition>();
		for (ModuleType type : ModuleType.values()) {
			results.addAll(findDefinitions(type));
		}
		return results;
	}

	@Override
	protected String inferModuleName(Resource resource) {
		Assert.isInstanceOf(NamedByteArrayResource.class, resource,
				"The passed in resource does not appear to have been constructed by this class");
		return ((NamedByteArrayResource) resource).name;
	}

	private BoundHashOperations<String, String, Object> hashOpsForType(ModuleType type) {
		return redisTemplate.boundHashOps("modules:" + type.name());
	}

	/**
	 * Used to remember the name of the module.
	 * 
	 * @author Eric Bottard
	 */
	private static class NamedByteArrayResource extends ByteArrayResource {

		private final String name;

		public NamedByteArrayResource(String name, byte[] byteArray) {
			super(byteArray);
			this.name = name;
		}


	}

	@Override
	public boolean isActive() {
		return false;
	}


}
