/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.config;

import org.springframework.context.ApplicationContext;
import org.springframework.integration.x.bus.LocalMessageBus;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.messaging.MessageChannel;


/**
 * 
 * @author David Turanski
 */
public class LocalSingleNodeInitializationTests extends AbstractSingleNodeInitializationTests {

	@Override
	protected void cleanup() {
	}

	@Override
	protected String getTransport() {
		return "local";
	}

	@Override
	protected Class<? extends MessageBus> getExpectedMessageBusType() {
		return LocalMessageBus.class;
	}

	@Override
	protected MessageChannel getControlChannel() {
		return context.getBean("containerControlChannel", MessageChannel.class);
	}

}
