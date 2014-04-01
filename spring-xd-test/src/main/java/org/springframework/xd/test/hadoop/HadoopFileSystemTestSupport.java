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

package org.springframework.xd.test.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import org.springframework.data.hadoop.test.context.HadoopCluster;
import org.springframework.data.hadoop.test.support.ClusterInfo;
import org.springframework.data.hadoop.test.support.HadoopClusterManager;
import org.springframework.xd.test.AbstractExternalResourceTestSupport;


/**
 * 
 * @author Gary Russell
 */
public class HadoopFileSystemTestSupport extends AbstractExternalResourceTestSupport<FileSystem> {

	private volatile Configuration configuration;

	public HadoopFileSystemTestSupport() {
		super("HADOOP_FS");
	}

	@Override
	protected void obtainResource() throws Exception {
		HadoopClusterManager manager = HadoopClusterManager.getInstance();
		HadoopCluster cluster = manager.getCluster(new ClusterInfo());
		cluster.start();
		this.configuration = cluster.getConfiguration();
		this.resource = cluster.getFileSystem();
	}

	@Override
	public void cleanupResource() throws Exception {
		this.resource.close();
		this.configuration.clear();
		HadoopClusterManager manager = HadoopClusterManager.getInstance();
		manager.close();
		this.resource = null;
		this.configuration = null;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

}
