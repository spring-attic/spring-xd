/*
 *
 *  * Copyright 2011-2014 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.xd.dirt.module.store;

import java.util.ArrayList;
import java.util.List;

import org.apache.zookeeper.KeeperException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.module.ModuleDependencyRepository;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.module.NoSuchModuleException;
import org.springframework.xd.dirt.module.WritableModuleRegistry;
import org.springframework.xd.dirt.module.support.ModuleDefinitionRepositoryUtils;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.CompositeModuleDefinition;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDefinitions;
import org.springframework.xd.module.ModuleType;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * An implementation of {@code WriteableModuleRegistry} dedicated to {@code CompositeModuleDefinition}s and that uses
 * ZooKeeper as storage mechanism.
 * <p>Writes each definition to a node, such as: {@code /xd/modules/[moduletype]/[modulename]} with the node data being
 * a JSON representation of the module definition.</p>
 *
 * @author Mark Fisher
 * @author David Turanski
 * @author Eric Bottard
 * @author Chris Lemper
 */
public class ZooKeeperComposedModuleDefinitionRegistry implements WritableModuleRegistry {

	private final ModuleDependencyRepository moduleDependencyRepository;

	/**
	 * A reference to the main module registry (which also typically contains this registry as a delegate),
	 * used to re-hydrate {@link org.springframework.xd.module.SimpleModuleDefinition}s with locations that
	 * make sense on this runtime(admin or container).
	 */
	private final ModuleRegistry mainModuleRegistry;

	private final ZooKeeperConnection zooKeeperConnection;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	public ZooKeeperComposedModuleDefinitionRegistry(
			ModuleDependencyRepository moduleDependencyRepository,
			ModuleRegistry mainModuleRegistry,
			ZooKeeperConnection zooKeeperConnection) {
		Assert.notNull(moduleDependencyRepository, "moduleDependencyRepository must not be null");
		Assert.notNull(zooKeeperConnection, "zooKeeperConnection must not be null");
		this.moduleDependencyRepository = moduleDependencyRepository;
		this.mainModuleRegistry = mainModuleRegistry;
		this.zooKeeperConnection = zooKeeperConnection;
	}


	@Override
	public boolean delete(ModuleDefinition definition) {
		Assert.notNull(definition, "'definition' cannot be null.");
		if (!definition.isComposed()) {
			return false;
		}
		String path = Paths.build(Paths.MODULES, definition.getType().toString(), definition.getName());
		try {
			// Delete actual definition
			zooKeeperConnection.getClient().delete().deletingChildrenIfNeeded().forPath(path);

			// As well as dependencies bookkeeping
			List<ModuleDefinition> children = ((CompositeModuleDefinition) definition).getChildren();
			for (ModuleDefinition child : children) {
				ModuleDefinitionRepositoryUtils.deleteDependencies(moduleDependencyRepository, child,
						dependencyKey(definition));
			}
		}
		catch (KeeperException.NoNodeException ignore) {
			// We are not responsible for this definition
			return false;
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
		return true;
	}

	@Override
	public boolean registerNew(ModuleDefinition definition) {
		if (!definition.isComposed()) {
			return false;
		}
		String path = Paths.build(Paths.MODULES, definition.getType().toString(),
				definition.getName());
		byte[] data = null;
		try {

			// Save actual definition
			data = objectMapper.writeValueAsString(definition).getBytes("UTF-8");
			zooKeeperConnection.getClient().create().creatingParentsIfNeeded().forPath(path, data);

			// Also track dependencies
			List<ModuleDefinition> childrenDefinitions = ((CompositeModuleDefinition) definition).getChildren();
			for (ModuleDefinition child : childrenDefinitions) {
				ModuleDefinitionRepositoryUtils.saveDependencies(moduleDependencyRepository, child,
						dependencyKey(definition));
			}
		}
		catch (KeeperException.NodeExistsException fallback) {
			try {
				zooKeeperConnection.getClient().setData().forPath(path, data);
			}
			catch (Exception e) {
				throw ZooKeeperUtils.wrapThrowable(e);
			}
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
		return true;
	}

	@Override
	public ModuleDefinition findDefinition(String name, ModuleType type) {
		String path = Paths.build(Paths.MODULES, type.toString(), name);
		try {
			byte[] data = zooKeeperConnection.getClient().getData().forPath(path);
			if (data.length == 0) {
				return null;
			}
			ModuleDefinition deserializedDefinition = this.objectMapper.readValue(new String(data, "UTF-8"),
					ModuleDefinition.class);
			return relookup(deserializedDefinition);
		}
		catch (Exception e) {
			// NoNodeException will return null
			ZooKeeperUtils.wrapAndThrowIgnoring(e, KeeperException.NoNodeException.class);
		}
		// non-composed module
		return null;
	}

	@Override
	public List<ModuleDefinition> findDefinitions(String name) {
		throw new UnsupportedOperationException("Not implemented (but never used)");
	}

	@Override
	public List<ModuleDefinition> findDefinitions(ModuleType type) {
		List<ModuleDefinition> results = new ArrayList<>();
		String path = Paths.build(Paths.MODULES, type.toString());
		try {
			List<String> children = zooKeeperConnection.getClient().getChildren().forPath(path);
			for (String child : children) {
				byte[] data = zooKeeperConnection.getClient().getData().forPath(
						Paths.build(Paths.MODULES, type.toString(), child));
				// Check for data (only composed modules have definitions)
				if (data != null && data.length > 0) {
					ModuleDefinition composed = this.findDefinition(child, type);
					if (composed != null) {
						results.add(composed);
					}
					else {
						throw new NoSuchModuleException(child, type);
					}
				}
			}
		}
		catch (Exception e) {
			ZooKeeperUtils.wrapAndThrowIgnoring(e, KeeperException.NoNodeException.class);
		}
		return results;
	}

	@Override
	public List<ModuleDefinition> findDefinitions() {
		List<ModuleDefinition> results = new ArrayList<>();
		for (ModuleType type : ModuleType.values()) {
			results.addAll(findDefinitions(type));
		}
		return results;
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
	 * Recursively re-lookup module definitions from the main module registry, so that locations for simple
	 * modules reflect the paths on the container/admin this code is running on.
	 *
	 * @param definition the ModuleDefinition to re-lookup
	 * @return module definition
	 */
	private ModuleDefinition relookup(ModuleDefinition definition) {
		if (!definition.isComposed()) {
			return mainModuleRegistry.findDefinition(definition.getName(), definition.getType());
		}
		else {
			List<ModuleDefinition> children = new ArrayList<>();
			CompositeModuleDefinition composite = (CompositeModuleDefinition) definition;
			for (ModuleDefinition child : composite.getChildren()) {
				children.add(relookup(child));
			}
			return ModuleDefinitions.composed(
					composite.getName(), composite.getType(), composite.getDslDefinition(), children);
		}
	}

}
