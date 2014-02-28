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

package org.springframework.xd.dirt.curator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.EnsurePath;

/**
 * Common paths and path utilities for XD components.
 * 
 * @author Patrick Peralta
 */
public class Paths {

	/**
	 * Map of paths to {@link org.apache.curator.utils.EnsurePath} instances.
	 */
	private static final ConcurrentMap<String, EnsurePath> ensurePaths = new ConcurrentHashMap<String, EnsurePath>();

	/**
	 * Namespace path (i.e. the top node in the hierarchy) for XD nodes.
	 */
	public static final String XD_NAMESPACE = "xd";

	/**
	 * Name of admin leader node.
	 */
	public static final String ADMIN = "admin";

	/**
	 * Name of deployments node. Deployments are written as children of this node.
	 */
	public static final String DEPLOYMENTS = "deployments";

	/**
	 * Name of containers node. Containers are written as children of this node.
	 */
	public static final String CONTAINERS = "containers";

	/**
	 * Name of streams node. Streams are written as children of this node.
	 */
	public static final String STREAMS = "streams";

	/**
	 * Strip path information from a string. For example, given an input of {@code /xd/path/location}, return
	 * {@code location}.
	 * 
	 * @param path path string
	 * 
	 * @return string with path stripped
	 */
	public static String stripPath(String path) {
		// todo: error handling
		return path.substring(path.lastIndexOf('/') + 1);
	}

	/**
	 * Ensure the existence of the given path.
	 * 
	 * @param client curator client
	 * @param path path to create, if needed
	 */
	public static void ensurePath(CuratorFramework client, String path) {
		EnsurePath ensurePath = ensurePaths.get(path);
		if (ensurePath == null) {
			ensurePaths.putIfAbsent(path, client.newNamespaceAwareEnsurePath(path));
			ensurePath = ensurePaths.get(path);
		}
		try {
			ensurePath.ensure(client.getZookeeperClient());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
