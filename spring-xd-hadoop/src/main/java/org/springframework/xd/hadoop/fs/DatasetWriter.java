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

package org.springframework.xd.hadoop.fs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.hadoop.store.dataset.DatasetOperations;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/**
 * 
 * @author Thomas Risberg
 */
public class DatasetWriter implements HdfsWriter {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private DatasetOperations datasetOperations;

	public DatasetWriter(DatasetOperations datasetOperations) {
		Assert.notNull(datasetOperations, "DatasetTemplate must not be null.");
		logger.info("Configured with datasetOperations: " + datasetOperations);
		this.datasetOperations = datasetOperations;
	}

	@Override
	public void write(Message<?> message) throws IOException {
		Object payload = message.getPayload();
		if (payload instanceof Collection<?>) {
			Collection<?> payloads = (Collection<?>) payload;
			if (logger.isDebugEnabled()) {
				logger.debug("Writing a collection of " + payloads.size() +
						" POJOs of type " + payloads.toArray()[0].getClass().getName());
			}
			datasetOperations.write((Collection<?>) message.getPayload());
		} else {
			logger.warn("Expected a collection of POJOs but received " + message.getPayload().getClass().getName());
			datasetOperations.write(Collections.singletonList(message.getPayload()));
		}
	}

	@Override
	public void close() {
		// no-op
	}

}
