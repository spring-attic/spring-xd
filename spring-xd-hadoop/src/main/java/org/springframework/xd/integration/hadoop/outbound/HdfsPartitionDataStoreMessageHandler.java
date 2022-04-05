/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.integration.hadoop.outbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.hadoop.store.PartitionDataStoreWriter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.util.Assert;


/**
 * Spring Integration {@code MessageHandler} handling {@code Message} writing into hdfs using {@code PartitionDataStoreWriter}.
 *
 * @author Janne Valkealahti
 */
public class HdfsPartitionDataStoreMessageHandler extends HdfsStoreMessageHandler {

	private static final Logger logger = LoggerFactory.getLogger(HdfsPartitionDataStoreMessageHandler.class);

	private PartitionDataStoreWriter<String, Message<?>> storePartitionWriter;

	/**
	 * Instantiates a new hdfs partition data store message handler.
	 *
	 * @param storePartitionWriter the store partition writer
	 */
	public HdfsPartitionDataStoreMessageHandler(PartitionDataStoreWriter<String, Message<?>> storePartitionWriter) {
		Assert.notNull(storePartitionWriter, "Writer must be set");
		this.storePartitionWriter = storePartitionWriter;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		Assert.notNull(storePartitionWriter, "Data Writer must be set");
	}

	@Override
	protected void doStop() {
		try {
			if (storePartitionWriter != null) {
				storePartitionWriter.close();
				storePartitionWriter = null;
			}
		}
		catch (Exception e) {
			logger.error("Error closing writer", e);
		}
	};

	@Override
	protected void doWrite(Message<?> message) throws Exception {
		Assert.notNull(storePartitionWriter, "Writer already closed");
		Object payload = message.getPayload();
		if (payload instanceof String) {
			storePartitionWriter.write((String) payload, message);
		}
		else {
			throw new MessageHandlingException(message,
					"message not a String");
		}
	}

}
