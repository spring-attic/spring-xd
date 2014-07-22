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

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;

import org.springframework.data.hadoop.fs.FsShell;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;


/**
 * HDFS shell utilities required for testing hdfs
 *
 * @author Glenn Renfro
 */

public class HadoopUtils {

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
	 * @param path the path
	 * @return collection of entries for the requested path
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
	public boolean fileExists(String uri) {
		Assert.hasText(uri, "uri can not be empty nor null");
		return shell.test(uri);
	}

	/**
	 * Recursively removes files in a directory or if only a file just that file.
	 *
	 * @param uri Path to the file or directory.
	 */
	public void fileRemove(String uri) {
		Assert.hasText(uri, "uri can not be empty nor null");
		shell.rmr(uri);
	}

	/**
	 * Retrieves the file from the hdfs file system.
	 *
	 * @param path The URI to the file to be retrieved.
	 * @return The string contents of the file.
	 */
	public String getFileContentsFromHdfs(String path) {
		Assert.hasText(path, "path can not be empty nor null");
		String result = null;
		RestTemplate template = new RestTemplate();
		String streamURL = String.format("http://%s:%s/streamFile%s?nnaddr=%s:%s",
				getNameNodeHost(), dataNodePort, path, getNameNodeHost(), getNameNodePort());
		result = template.getForObject(streamURL, String.class);
		return result;
	}

	/**
	 * Waits up to the timeout for the resource to be written to hdfs.
	 *
	 * @param waitTime The number of millis to wait.
	 * @param path the path to the resource .
	 * @return false if the path was not present. True if it was present.
	 */
	public boolean waitForPath(int waitTime, String path) {
		long timeout = System.currentTimeMillis() + waitTime;
		boolean exists = fileExists(path);
		while (!exists && System.currentTimeMillis() < timeout) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e.getMessage(), e);
			}
			exists = fileExists(path);
		}
		return exists;
	}

	/**
	 * Retrieves the file status for the file path specified.
	 * @param filePath path/file name of the file to interrogate
	 * @return the FileStatus for the file.
	 */
	public FileStatus getFileStatus(String filePath) {
		Collection<FileStatus> fileStatuses = listDir(filePath);
		assertEquals("The number of files in list result should only be 1. The file itself. ", 1,
				fileStatuses.size());
		return fileStatuses.iterator().next();
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
