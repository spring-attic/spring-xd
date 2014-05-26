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

package org.springframework.xd.integration.hadoop.config;

import org.springframework.data.hadoop.store.DataStoreWriter;
import org.springframework.data.hadoop.store.PartitionDataStoreWriter;
import org.springframework.integration.config.AbstractSimpleMessageHandlerFactoryBean;
import org.springframework.util.Assert;
import org.springframework.xd.integration.hadoop.outbound.HdfsDataStoreMessageHandler;
import org.springframework.xd.integration.hadoop.outbound.HdfsPartitionDataStoreMessageHandler;
import org.springframework.xd.integration.hadoop.outbound.HdfsStoreMessageHandler;

/**
 * A message handler factory bean which differentiates whether {@link HdfsStoreMessageHandler}
 * should be based on either {@link HdfsPartitionDataStoreMessageHandler} or {@link HdfsDataStoreMessageHandler}.
 * Choice of which {@link HdfsStoreMessageHandler} is created depends whether {@link DataStoreWriter} is
 * {@link PartitionDataStoreWriter} or not.
 *
 * @author Janne Valkealahti
 */
public class HdfsStoreMessageHandlerFactoryBean extends
		AbstractSimpleMessageHandlerFactoryBean<HdfsStoreMessageHandler> {

	private boolean autoStartup = true;

	private DataStoreWriter<String> storeWriter;

	/**
	 * Sets the auto startup.
	 *
	 * @param autoStartup the new auto startup
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * Sets the store writer.
	 *
	 * @param storeWriter the new store writer
	 */
	public void setStoreWriter(DataStoreWriter<String> storeWriter) {
		this.storeWriter = storeWriter;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected HdfsStoreMessageHandler createHandler() {
		Assert.notNull(storeWriter, "Writer cannot be null for this factory");
		HdfsStoreMessageHandler handler = null;
		if (storeWriter instanceof PartitionDataStoreWriter) {
			handler = new HdfsPartitionDataStoreMessageHandler((PartitionDataStoreWriter) storeWriter);
		}
		else {
			handler = new HdfsDataStoreMessageHandler(storeWriter);
		}
		handler.setAutoStartup(autoStartup);
		return handler;
	}

}
