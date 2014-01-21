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

import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import org.springframework.data.hadoop.configuration.ConfigurationFactoryBean;
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
		ConfigurationFactoryBean cfb = new ConfigurationFactoryBean();
		Properties props = new Properties();
		props.setProperty("fs.default.name", "hdfs://localhost:8020");
		props.setProperty("ipc.client.connect.max.retries", "1");
		cfb.setProperties(props);
		cfb.setRegisterUrlHandler(false);
		cfb.afterPropertiesSet();
		this.configuration = cfb.getObject();
		this.resource = FileSystem.get(this.configuration);
	}

	@Override
	public void cleanupResource() throws Exception {
		this.resource.close();
		this.configuration.clear();
	}

	public Configuration getConfiguration() {
		return configuration;
	}

}
