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

package org.springframework.xd.dirt.modules.metadata;

import javax.validation.constraints.AssertTrue;

import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;


/**
 * Options metadata for Redis sink module.
 *
 * @author Ilayaperumal Gopinathan
 */
@Mixin(RedisConnectionMixin.class)
public class RedisSinkOptionsMetadata implements ProfileNamesProvider {

	private static final String TOPIC_PROFILE = "use-topic";

	private static final String QUEUE_PROFILE = "use-queue";

	private static final String KEY_PROFILE = "use-store";

	private String topic = null;

	private String queue = null;

	private String key = null;

	private String collectionType = "LIST";

	public String getTopic() {
		return this.topic;
	}

	@ModuleOption("spel expression to use for topic")
	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getQueue() {
		return this.queue;
	}

	@ModuleOption("spel expression to use for queue")
	public void setQueue(String queue) {
		this.queue = queue;
	}


	public String getKey() {
		return this.key;
	}

	@ModuleOption("name of the redis store key to use")
	public void setKey(String key) {
		this.key = key;
	}

	public String getCollectionType() {
		return collectionType;
	}


	@ModuleOption("the collection type to use for the given key")
	public void setCollectionType(String collectionType) {
		this.collectionType = collectionType;
	}

	/**
	 * User can't explicitly set mutually exclusive values together.
	 */
	@AssertTrue(message = "the 'topic', 'queue' and 'key' options are mutually exclusive")
	public boolean isMutuallyExclusive() {
		return (topic == null || queue != null || key == null) ||
				(key == null || topic != null || queue == null) ||
				(queue == null || key != null || topic == null);
	}

	@AssertTrue(message = "one of the 'topic', 'queue' or 'key' values must be set explicitly")
	public boolean isValid() {
		return ((queue != null) || (topic != null) || (key != null));
	}

	@Override
	public String[] profilesToActivate() {
		if (topic != null) {
			return new String[] { TOPIC_PROFILE };
		}
		else if (queue != null) {
			return new String[] { QUEUE_PROFILE };
		}
		else if (key != null) {
			return new String[] { KEY_PROFILE };
		}
		return new String[] {};
	}
}
