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

package org.springframework.xd.integration.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.hadoop.fs.FsShell;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;


/**
 * HDFS shell utilities required for testing hdfs
 *
 * @author Glenn Renfro
 */

public class HadoopUtils {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(HadoopUtils.class);

	private FsShell shell;

	private String nameNode;

	private String dataNodePort;

	private Configuration hadoopConfiguration;

	/**
	 * Initializes the hadoop utils with the name node that was specified in the environment.
	 *
	 * @param xdEnvironment the environment for the test.
	 */
	public HadoopUtils(XdEnvironment xdEnvironment) {
		Assert.notNull(xdEnvironment, "xdEnvironment can not be null");
		this.nameNode = xdEnvironment.getNameNode();

		hadoopConfiguration = new Configuration();
		FileSystem.setDefaultUri(hadoopConfiguration, nameNode);
		shell = new FsShell(hadoopConfiguration);
		dataNodePort = xdEnvironment.getDataNodePort();
	}

	/**
	 * Retrieves a list of entries in the hdfs at the path specified.
	 *
	 * @param path The path
	 * @return
	 */
	public Collection<FileStatus> listDir(String path) {
		Assert.hasText(path, "path can not be empty nor null");
		return shell.ls(path);
	}

	/**
	 * Verifies that the file exists.
	 *
	 * @param uri The uri of the file to verify.
	 * @return True if it exists else false.
	 */
	public boolean test(String uri) {
		Assert.hasText(uri, "uri can not be empty nor null");
		return shell.test(uri);
	}

	/**
	 * Recursively removes files in a directory or if only a file just that file.
	 *
	 * @param uri Path to the file or directory.
	 */
	public void rmr(String uri) {
		Assert.hasText(uri, "uri can not be empty nor null");
		shell.rmr(uri);
	}

	/**
	 * Retrieves the file from the hdfs file system.
	 *
	 * @param path The URI to the file to be retrieved.
	 * @return The string contents of the file.
	 */
	public String getTestContent(String path) {
		Assert.hasText(path, "path can not be empty nor null");
		String result = null;
		RestTemplate template = new RestTemplate();
		String streamURL = String.format("http://%s:%s/streamFile%s?nnaddr=%s:%s",
				getNameNodeHost(), dataNodePort, path, getNameNodeHost(), getNameNodePort());
		result = template.getForObject(streamURL, String.class);
		return result;
	}

	/**
	 * Returns the host of the name node.
	 *
	 * @return String representation of the host.
	 */
	private String getNameNodeHost() {
		try {
			URI uri = new URI(nameNode);
			return uri.getHost();
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

	}

	/**
	 * Returns the port of the name node.
	 *
	 * @return name nodes port.
	 */
	private int getNameNodePort() {
		try {
			URI uri = new URI(nameNode);
			return uri.getPort();
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

	}

}
