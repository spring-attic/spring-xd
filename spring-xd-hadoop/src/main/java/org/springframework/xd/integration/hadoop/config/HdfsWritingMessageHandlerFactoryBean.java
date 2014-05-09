/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.integration.hadoop.config;

import org.apache.hadoop.fs.FileSystem;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;
import org.springframework.xd.hadoop.fs.HdfsTextFileWriterFactory;
import org.springframework.xd.integration.hadoop.outbound.HdfsWritingMessageHandler;

/**
 * Factory bean used to create {@link HdfsWritingMessageHandler}
 * 
 * @author Mark Fisher
 */
public class HdfsWritingMessageHandlerFactoryBean implements FactoryBean<HdfsWritingMessageHandler> {

	private final FileSystem fileSystem;

	private volatile String basePath;

	private volatile String baseFilename;

	private volatile String fileSuffix;

	private volatile long rolloverThresholdInBytes;

	private volatile Boolean autoStartup;

	private volatile HdfsWritingMessageHandler handler;

	public HdfsWritingMessageHandlerFactoryBean(FileSystem fileSystem) {
		Assert.notNull(fileSystem, "fileSystem must not be null");
		this.fileSystem = fileSystem;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public void setBaseFilename(String baseFilename) {
		this.baseFilename = baseFilename;
	}

	public void setFileSuffix(String fileSuffix) {
		this.fileSuffix = fileSuffix;
	}

	public void setRolloverThresholdInBytes(long rolloverThresholdInBytes) {
		this.rolloverThresholdInBytes = rolloverThresholdInBytes;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public Class<?> getObjectType() {
		return HdfsWritingMessageHandler.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public synchronized HdfsWritingMessageHandler getObject() throws Exception {
		if (handler == null) {
			HdfsTextFileWriterFactory writerFactory = new HdfsTextFileWriterFactory(this.fileSystem);
			writerFactory.setBasePath(this.basePath);
			writerFactory.setBaseFilename(this.baseFilename);
			writerFactory.setFileSuffix(fileSuffix);
			writerFactory.setRolloverThresholdInBytes(rolloverThresholdInBytes);
			this.handler = new HdfsWritingMessageHandler(writerFactory);
			if (this.autoStartup != null) {
				this.handler.setAutoStartup(this.autoStartup);
			}
		}
		return this.handler;
	}

}
