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

package org.springframework.xd.dirt.plugins;

import java.util.Date;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class TestProducer implements InitializingBean {

	private final MessageChannel output;

	private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

	public TestProducer(MessageChannel output) {
		this.output = output;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		scheduler.afterPropertiesSet();
		scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				output.send(MessageBuilder.withPayload("hello").build());
			}
		}, new Date(System.currentTimeMillis() + 3000));
	}

}
