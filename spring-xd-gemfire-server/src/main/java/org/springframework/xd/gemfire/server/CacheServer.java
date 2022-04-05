/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.gemfire.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.management.CacheServerMXBean;
import com.gemstone.gemfire.management.ManagementService;

/**
 * A simple driver class to run a Gemfire cache server.
 *
 * @author David Turanski
 */
public class CacheServer {

	private static final Logger logger = LoggerFactory.getLogger(CacheServer.class);

	private static final int port = 40404;

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: CacheServer <config-file-path>");
			System.exit(1);
		}

		/*
		 * Ensure absolute path is handled
		 */
		String path = args[0];
		if (!path.startsWith("file:")) {
			path = "file:" + path;
		}

		if (!portAvailable(port)) {
			System.out.println("Cache Server port " + port + " is not available. Is another instance running?");
			System.exit(1);
		}
		logger.info("Starting Cache Server");
		@SuppressWarnings("resource")
		FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext(path);
		context.registerShutdownHook();
		Cache cache = context.getBean(Cache.class);
		ManagementService ms = ManagementService.getExistingManagementService(cache);
		CacheServerMXBean cacheServerManager = ms.getLocalCacheServerMXBean(port);
		if (!cacheServerManager.isRunning()) {
			System.out.println("failed to start cache server ");
			System.exit(1);
		}
		System.out.println("cache server running");
	}

	public static boolean portAvailable(int port) {
		InetSocketAddress addr = new InetSocketAddress(port);
		try {
			ServerSocket socket = new ServerSocket();
			socket.bind(addr);
			socket.close();
		}
		catch (IOException e) {
			return false;
		}
		return true;

	}
}
