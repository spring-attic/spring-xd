/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.module.config;

import java.util.Map;

import org.springframework.integration.MessageChannel;

/**
 * Strategy interface to expose module inputs and outputs
 *
 * @author David Turanski
 */
public interface ChannelExporter {

	public MessageChannel getInputChannel(Map<String,MessageChannel> channels);

	public Map<String,MessageChannel> getOutputChannels(Map<String,MessageChannel> channels);

	public <T extends MessageChannel> Map<String, T> getOutputChannels(Map<String,MessageChannel> channels, Class<T> requiredType);

}
