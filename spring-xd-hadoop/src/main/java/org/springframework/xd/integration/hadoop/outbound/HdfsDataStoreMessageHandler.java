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

package org.springframework.xd.integration.hadoop.outbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.hadoop.store.DataStoreWriter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.util.Assert;

/**
 * Spring Integration {@code MessageHandler} handling {@code Message} writing into hdfs using {@code DataStoreWriter}.
 *
 * @author Janne Valkealahti
 */
public class HdfsDataStoreMessageHandler extends HdfsStoreMessageHandler {

	private static final Logger logger = LoggerFactory.getLogger(HdfsDataStoreMessageHandler.class);

	private DataStoreWriter<String> storeWriter;

	/**
	 * Instantiates a new hdfs data store message handler.
	 *
	 * @param storeWriter the store writer
	 */
	public HdfsDataStoreMessageHandler(DataStoreWriter<String> storeWriter) {
		Assert.notNull(storeWriter, "Writer must be set");
		this.storeWriter = storeWriter;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		Assert.notNull(storeWriter, "Data Writer must be set");
	}

	@Override
	protected void doStop() {
		try {
			if (storeWriter != null) {
				storeWriter.close();
				storeWriter = null;
			}
		}
		catch (Exception e) {
			logger.error("Error closing writer", e);
		}
	};

	@Override
	protected void doWrite(Message<?> message) throws Exception {
		Assert.notNull(storeWriter, "Writer already closed");
		Object payload = message.getPayload();
		if (payload instanceof String) {
			storeWriter.write((String) payload);
		}
		else {
			throw new MessageHandlingException(message,
					"message not a String");
		}
	}

}
