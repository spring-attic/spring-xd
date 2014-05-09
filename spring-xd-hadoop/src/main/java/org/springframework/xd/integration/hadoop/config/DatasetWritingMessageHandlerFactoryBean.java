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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.data.hadoop.store.dataset.DatasetOperations;
import org.springframework.util.Assert;
import org.springframework.xd.hadoop.fs.DatasetWriterFactory;
import org.springframework.xd.integration.hadoop.outbound.HdfsWritingMessageHandler;

/**
 * Factory bean used to create {@link HdfsWritingMessageHandler}
 * 
 * @author Thomas Risberg
 */
public class DatasetWritingMessageHandlerFactoryBean implements FactoryBean<HdfsWritingMessageHandler> {

	private final DatasetOperations datasetOperations;

	private volatile Boolean autoStartup;

	private volatile HdfsWritingMessageHandler handler;

	public DatasetWritingMessageHandlerFactoryBean(DatasetOperations datasetOperations) {
		Assert.notNull(datasetOperations, "datasetOperations must not be null");
		this.datasetOperations = datasetOperations;
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
			DatasetWriterFactory writerFactory = new DatasetWriterFactory(this.datasetOperations);
			this.handler = new HdfsWritingMessageHandler(writerFactory);
			if (this.autoStartup != null) {
				this.handler.setAutoStartup(this.autoStartup);
			}
		}
		return this.handler;
	}

}
