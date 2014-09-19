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

import org.springframework.data.redis.support.collections.RedisCollectionFactoryBean;
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

	private static final String TOPIC_EXPRESSION_PROFILE = "use-topic-expression";

	private static final String QUEUE_EXPRESSION_PROFILE = "use-queue-expression";

	private static final String STORE_EXPRESSION_PROFILE = "use-store-expression";

	private String topic = null;

	private String queue = null;

	private String key = null;

	private String topicExpression = null;

	private String queueExpression = null;

	private String keyExpression = null;

	private RedisCollectionFactoryBean.CollectionType collectionType = RedisCollectionFactoryBean.CollectionType.LIST;

	public String getTopicExpression() {
		return this.topicExpression;
	}

	@ModuleOption("a SpEL expression to use for topic")
	public void setTopicExpression(String topicExpression) {
		this.topicExpression = topicExpression;
	}

	public String getQueueExpression() {
		return this.queueExpression;
	}

	@ModuleOption("a SpEL expression to use for queue")
	public void setQueueExpression(String queueExpression) {
		this.queueExpression = queueExpression;
	}


	public String getKeyExpression() {
		return this.keyExpression;
	}

	@ModuleOption("a SpEL expression to use for keyExpression")
	public void setKeyExpression(String keyExpression) {
		this.keyExpression = keyExpression;
	}

	public String getTopic() {
		return topic;
	}

	@ModuleOption("name for the topic")
	public void setTopic(String topic) {
		this.topic = topic;
		this.topicExpression = "'" + this.topic + "'";
	}

	public String getQueue() {
		return queue;
	}

	@ModuleOption("name for the queue")
	public void setQueue(String queue) {
		this.queue = queue;
		this.queueExpression = "'" + queue + "'";
	}

	public String getKey() {
		return key;
	}

	@ModuleOption("name for the key")
	public void setKey(String key) {
		this.key = key;
		this.keyExpression = "'" + this.key + "'";
	}

	public RedisCollectionFactoryBean.CollectionType getCollectionType() {
		return collectionType;
	}


	@ModuleOption("the collection type to use for the given key")
	public void setCollectionType(RedisCollectionFactoryBean.CollectionType collectionType) {
		this.collectionType = collectionType;
	}

	/**
	 * User can't explicitly set mutually exclusive values together.
	 */
	@AssertTrue(message = "the 'topic', 'topicExpression', 'queue', 'queueExpression', 'key' and 'keyExpression' options are mutually exclusive")
	public boolean isOptionMutuallyExclusive() {
		boolean optionSpecified = false;
		String[] distinctOptions = { this.topicExpression, this.queueExpression, this.keyExpression };
		for (String option : distinctOptions) {
			if (optionSpecified == true && option != null) {
				return false;
			}
			if (option != null) {
				optionSpecified = true;
			}
		}
		return (checkMutuallyExclusive(this.queue, this.queueExpression)
				&& checkMutuallyExclusive(this.topic, this.topicExpression) && checkMutuallyExclusive(this.key,
					this.keyExpression));
	}

	/**
	 * Check if the literal and expression values mutually exclusive (have different content).
	 *
	 * @param literal
	 * @param expression
	 * @return boolean value
	 */
	private boolean checkMutuallyExclusive(String literal, String expression) {
		return (expression == null || literal == null) ? true
				: (expression.contains(literal) && (literal.length() + 2 == expression.length()));
	}

	@AssertTrue(message = "one of 'topic', 'topicExpression', 'queue', 'queueExpression', 'key', 'keyExpression' options must be set explicitly")
	public boolean isOptionRequired() {
		return ((queue != null) || (topic != null) || (key != null) || (queueExpression != null)
				|| (topicExpression != null) || (keyExpression != null));
	}

	@AssertTrue(message = "collection type is not valid")
	public boolean isCollectionTypeValid() {
		for (RedisCollectionFactoryBean.CollectionType type : RedisCollectionFactoryBean.CollectionType.values()) {
			if (type.name().equals(this.collectionType.name())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String[] profilesToActivate() {
		if (topicExpression != null) {
			return new String[] { TOPIC_EXPRESSION_PROFILE };
		}
		else if (queueExpression != null) {
			return new String[] { QUEUE_EXPRESSION_PROFILE };
		}
		else if (keyExpression != null) {
			return new String[] { STORE_EXPRESSION_PROFILE };
		}
		return new String[] {};
	}
}
