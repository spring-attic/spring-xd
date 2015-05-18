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

package org.springframework.xd.module.options.mixins;

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Mixin for polled sources where the default is 1 message per poll.
 *
 * @author Gary Russell
 */
public class MaxMessagesDefaultOneMixin {

	long maxMessages = 1;

	public long getMaxMessages() {
		return this.maxMessages;
	}

	@ModuleOption("the maximum messages per poll; -1 for unlimited")
	public void setMaxMessages(long maxMessages) {
		this.maxMessages = maxMessages;
	}

}
