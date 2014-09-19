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

import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ModulePlaceholders;


/**
 * Options metadata for Redis sink module.
 *
 * @author Ilayaperumal Gopinathan
 */
@Mixin(RedisConnectionMixin.class)
public class RedisSinkOptionsMetadata {

	private String topic = "'" + ModulePlaceholders.XD_STREAM_NAME + "'";

	public String getTopic() {
		return topic;
	}

	@ModuleOption("spel expression to use for topic")
	public void setTopic(String topic) {
		this.topic = topic;
	}
}
