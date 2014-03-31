/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.module.store;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDependencyRepository;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.module.support.ModuleDefinitionRepositoryUtils;
import org.springframework.xd.dirt.stream.redis.ModuleDefinitionMixin;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A ZooKeeper based store of {@link ModuleDefinition}s that writes each definition to a node, such as:
 * {@code /xd/modules/[moduletype]/[modulename]}.
 * 
 * @author Mark Fisher
 */
public class ZooKeeperModuleDefinitionRepository implements ModuleDefinitionRepository {

	private static final String MODULES_NODE = "modules";

	private final ModuleRegistry moduleRegistry;

	private final ModuleDependencyRepository moduleDependencyRepository;

	private final ZooKeeperConnection zooKeeperConnection;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	public ZooKeeperModuleDefinitionRepository(ModuleRegistry moduleRegistry,
			ModuleDependencyRepository moduleDependencyRepository,
			ZooKeeperConnection zooKeeperConnection) {
		Assert.notNull(moduleRegistry, "moduleRegistry must not be null");
		Assert.notNull(moduleDependencyRepository, "moduleDependencyRepository must not be null");
		Assert.notNull(zooKeeperConnection, "zooKeeperConnection must not be null");
		this.moduleRegistry = moduleRegistry;
		this.moduleDependencyRepository = moduleDependencyRepository;
		this.zooKeeperConnection = zooKeeperConnection;
		objectMapper.addMixInAnnotations(ModuleDefinition.class, ModuleDefinitionMixin.class);
	}

	@Override
	public ModuleDefinition findByNameAndType(String name, ModuleType type) {
		Assert.notNull(type, "type is required");
		ModuleDefinition definition = moduleRegistry.findDefinition(name, type);
		if (definition == null) {
			String path = Paths.build(MODULES_NODE, type.toString(), name);
			try {
				byte[] data = zooKeeperConnection.getClient().getData().forPath(path);
				ModuleDefinition shallowValue = this.objectMapper.readValue(new String(data, "UTF-8"),
						ModuleDefinition.class);
				List<ModuleDefinition> deepModules = new ArrayList<ModuleDefinition>(
						shallowValue.getComposedModuleDefinitions().size());
				for (ModuleDefinition child : shallowValue.getComposedModuleDefinitions()) {
					deepModules.add(findByNameAndType(child.getName(), child.getType()));
				}
				shallowValue.setComposedModuleDefinitions(deepModules);
				definition = shallowValue;
			}
			catch (NoNodeException e) {
				// will return null
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return definition;
	}

	@Override
	public Page<ModuleDefinition> findByType(Pageable pageable, ModuleType type) {
		if (type == null) {
			return findAll(pageable);
		}
		List<ModuleDefinition> results = new ArrayList<ModuleDefinition>();
		results.addAll(moduleRegistry.findDefinitions(type));
		String path = Paths.build(MODULES_NODE, type.toString());
		try {
			List<String> children = zooKeeperConnection.getClient().getChildren().forPath(path);
			for (String child : children) {
				byte[] data = zooKeeperConnection.getClient().getData().forPath(
						Paths.build(MODULES_NODE, type.toString(), child));
				// Check for data (only composed modules have definitions)
				if (data != null && data.length > 0) {

					ModuleDefinition composed = this.findByNameAndType(child, type);
					if (composed != null) {
						results.add(composed);
					}
				}
			}
		}
		catch (Exception e) {
			// continue
		}
		Assert.isNull(pageable.getSort(), "Arbitrary sorting is not implemented");
		return slice(results, pageable);
	}

	private Page<ModuleDefinition> findAll(Pageable pageable) {
		List<ModuleDefinition> results = new ArrayList<ModuleDefinition>();
		for (ModuleType type : ModuleType.values()) {
			results.addAll(findByType(pageable, type).getContent());
		}
		return slice(results, pageable);
	}

	@Override
	public Set<String> findDependentModules(String name, ModuleType type) {
		Set<String> dependentModules = moduleDependencyRepository.find(name, type);
		if (dependentModules == null) {
			return new HashSet<String>();
		}
		return dependentModules;
	}

	@Override
	public ModuleDefinition save(ModuleDefinition moduleDefinition) {
		String def = moduleDefinition.getDefinition();
		if (def != null) {
			String path = Paths.build(MODULES_NODE, moduleDefinition.getType().toString(),
					moduleDefinition.getName());
			byte[] data = null;
			try {
				data = objectMapper.writeValueAsString(moduleDefinition).getBytes("UTF-8");
				zooKeeperConnection.getClient().create().creatingParentsIfNeeded().forPath(path, data);
			}
			catch (NodeExistsException fallback) {
				try {
					zooKeeperConnection.getClient().setData().forPath(path, data);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		for (ModuleDefinition child : moduleDefinition.getComposedModuleDefinitions()) {
			ModuleDefinitionRepositoryUtils.saveDependencies(moduleDependencyRepository, child,
					dependencyKey(moduleDefinition));
		}
		return moduleDefinition;
	}

	@Override
	public void delete(String name, ModuleType type) {
		ModuleDefinition definition = this.findByNameAndType(name, type);
		if (definition != null) {
			this.delete(definition);
		}
	}

	@Override
	public void delete(ModuleDefinition moduleDefinition) {
		String path = Paths.build(MODULES_NODE, moduleDefinition.getType().toString(), moduleDefinition.getName());
		try {
			zooKeeperConnection.getClient().delete().deletingChildrenIfNeeded().forPath(path);
			for (ModuleDefinition composedModule : moduleDefinition.getComposedModuleDefinitions()) {
				ModuleDefinitionRepositoryUtils.deleteDependencies(moduleDependencyRepository, composedModule,
						dependencyKey(moduleDefinition));
			}
		}
		catch (NoNodeException e) {
			// nothing to delete
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Generates the key used in the ModuleDependencyRepository.
	 * 
	 * @param moduleDefinition the moduleDefinition being saved or deleted
	 * @return generated key
	 */
	private String dependencyKey(ModuleDefinition moduleDefinition) {
		return String.format("module:%s:%s", moduleDefinition.getType(), moduleDefinition.getName());
	}

	/**
	 * Post-process the list to only return elements matching the page request.
	 */
	private Page<ModuleDefinition> slice(List<ModuleDefinition> list, Pageable pageable) {
		int to = Math.min(list.size(), pageable.getOffset() + pageable.getPageSize());
		List<ModuleDefinition> data = list.subList(pageable.getOffset(), to);
		return new PageImpl<ModuleDefinition>(data, pageable, list.size());
	}

}
