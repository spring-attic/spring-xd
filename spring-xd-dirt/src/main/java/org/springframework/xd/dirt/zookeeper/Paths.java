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

package org.springframework.xd.dirt.zookeeper;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.EnsurePath;

import org.springframework.xd.dirt.module.store.ModuleMetadata;

/**
 * Common paths and path utilities for XD components.
 *
 * @author Patrick Peralta
 * @author David Turanski
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
	 * Name of admins (that could participate to become leader) node.
	 * Admin lock nodes are written as children of this node. This node
	 * is solely used from curator.
	 */
	public static final String ADMINELECTION = "adminelection";

	/**
	 * Name of admins node. Containers are written as children of this node.
	 * Used to store admin metadata while curator election is handled under
	 * adminelection.
	 */
	public static final String ADMINS = "admins";

	/**
	 * Name of containers node. Containers are written as children of this node.
	 */
	public static final String CONTAINERS = "containers";

	/**
	 * Name of modules node. Module definitions are written as children of this node.
	 */
	public static final String MODULES = "modules";

	/**
	 * Name of streams node. Streams are written as children of this node.
	 */
	public static final String STREAMS = "streams";

	/**
	 * Name of jobs node. Jobs are written as children of this node.
	 */
	public static final String JOBS = "jobs";

	/**
	 * Name of taps node. Channel names with active taps are written as children of this node.
	 */
	public static final String TAPS = "taps";

	/**
	 * Name of deployments node. Deployments are written as children of this node.
	 */
	public static final String DEPLOYMENTS = "deployments";

	/**
	 * Name of the deployment modules node that are allocated to containers.
	 * Allocated modules are written as children of this node.
	 */
	public static final String ALLOCATED = "allocated";

	/**
	 * Name of metadata node. The data for this node contains {@link ModuleMetadata}.
	 */
	public static final String METADATA = "metadata";

	/**
	 * Name of the module deployment requests node. Requested modules are written as children of this node.
	 */
	public static final String REQUESTED = "requested";

	/**
	 * Name of status node. The data for this node contains status information.
	 */
	public static final String STATUS = "status";

	/**
	 * Name of the queue node.
	 */
	public static final String QUEUE = "queue";

	/**
	 * Name of the parent node for deployment request responses.
	 */
	public static final String RESPONSES = "responses";

	/**
	 * Name of module deployments node. Module deployment requests for
	 * individual containers are written as children of this node.
	 */
	public static final String MODULE_DEPLOYMENTS = DEPLOYMENTS + '/' + MODULES;

	/**
	 * Name of stream deployments node. Stream deployment requests are written
	 * as children of this node.
	 */
	public static final String STREAM_DEPLOYMENTS = DEPLOYMENTS + '/' + STREAMS;

	/**
	 * Name of job deployments node. Job deployment requests are written
	 * as children of this node.
	 */
	public static final String JOB_DEPLOYMENTS = DEPLOYMENTS + '/' + JOBS;

	/**
	 * Name of the stream deployment queue path.
	 */
	public static final String DEPLOYMENT_QUEUE = QUEUE  + '/' + DEPLOYMENTS;

	/**
	 * Strip path information from a string. For example, given an input of
	 * {@code /xd/path/location}, return {@code location}.
	 *
	 * @param path path string
	 *
	 * @return string with path stripped
	 */
	public static String stripPath(String path) {
		int i = path.lastIndexOf('/');
		return i > -1 ? path.substring(i + 1) : path;
	}

	/**
	 * Return a string with the provided path elements separated by a slash {@code /}.
	 * The leading slash is created if required.
	 *
	 * @param elements path elements
	 *
	 * @return the full path
	 */
	public static String build(String... elements) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < elements.length; i++) {
			if (i == 0 && elements[i].charAt(0) != '/') {
				builder.append('/');
			}
			builder.append(elements[i]);
			if (i + 1 < elements.length) {
				builder.append('/');
			}
		}

		return builder.toString();
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
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

}
