/*
 * Copyright 2015 the original author or authors.
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


package org.springframework.integration.x.kafka;

import java.util.Map;

import kafka.api.OffsetRequest;

import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.listener.MetadataStoreOffsetManager;

/**
 * @author Marius Bogoevici
 */
public class SpringXdOffsetManager extends MetadataStoreOffsetManager {

	public SpringXdOffsetManager(ConnectionFactory connectionFactory) {
		super(connectionFactory);
	}

	public SpringXdOffsetManager(ConnectionFactory connectionFactory, Map<Partition, Long> initialOffsets) {
		super(connectionFactory, initialOffsets);
	}

	public void deleteOffset(Partition partition) {
		getMetadataStore().remove(partition.getTopic() + ":" + partition.getId() + ":" + getConsumerId());
	}

	public void setAutoOffsetReset(AutoOffsetResetStrategy autoOffsetResetStrategy) {
		this.setReferenceTimestamp(autoOffsetResetStrategy.code());
	}

	public static enum AutoOffsetResetStrategy {

		smallest(OffsetRequest.EarliestTime()),
		largest(OffsetRequest.LatestTime());

		private long code;

		AutoOffsetResetStrategy(long code) {
			this.code = code;
		}

		public long code() {
			return code;
		}
	}

}
