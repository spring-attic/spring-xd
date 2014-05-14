/*
 * Copyright 2014 the original author or authors.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.xd.dirt.module.ModuleDependencyRepository;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.ModuleType;

/**
 * ZooKeeper based implementation of {@link ModuleDependencyRepository} that writes each dependency to a node, such as:
 * {@code /xd/modules/[moduletype]/[modulename]/dependencies/[target]}.
 * 
 * @author Mark Fisher
 * @author David Turanski
 */
public class ZooKeeperModuleDependencyRepository implements ModuleDependencyRepository {

	private static final String MODULES_NODE = "modules";

	private static final String DEPENDENCIES_NODE = "dependencies";

	private final ZooKeeperConnection connection;

	@Autowired
	public ZooKeeperModuleDependencyRepository(ZooKeeperConnection connection) {
		this.connection = connection;
	}

	@Override
	public void store(String moduleName, ModuleType type, String target) {
		String path = Paths.build(MODULES_NODE, type.toString(), moduleName, DEPENDENCIES_NODE, target);
		try {
			connection.getClient().create().creatingParentsIfNeeded().forPath(path);
		}
		catch (Exception e) {
			// NodeExistsException - already created
			ZooKeeperUtils.wrapAndThrowIgnoring(e, NodeExistsException.class);
		}
	}

	@Override
	public Set<String> find(String name, ModuleType type) {
		String path = Paths.build(MODULES_NODE, type.toString(), name, DEPENDENCIES_NODE);
		try {
			List<String> results = connection.getClient().getChildren().forPath(path);
			return (results != null) ? new HashSet<String>(results) : Collections.<String> emptySet();
		}
		catch (NoNodeException e) {
			return Collections.emptySet();
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	@Override
	public void delete(String module, ModuleType type, String target) {
		String path = Paths.build(MODULES_NODE, type.toString(), module, DEPENDENCIES_NODE, target);
		try {
			connection.getClient().delete().forPath(path);
		}
		catch (Exception e) {
			//NoNodeException - nothing to delete
			ZooKeeperUtils.wrapAndThrowIgnoring(e, NoNodeException.class);
		}
	}

}
