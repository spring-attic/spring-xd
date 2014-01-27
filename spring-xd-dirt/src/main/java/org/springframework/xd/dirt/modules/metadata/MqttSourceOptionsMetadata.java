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

package org.springframework.xd.dirt.modules.metadata;

import javax.validation.constraints.Size;

import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Describes options to the {@code mqtt} source module.
 * 
 * @author Eric Bottard
 */
public class MqttSourceOptionsMetadata {

	private String clientId;

	private String url;

	private String topics;

	private String username;

	private String password;

	// Not putting @NotNull throughout for now, as defaults
	// may come from properties file
	// @NotNull
	@Size(min = 1, max = 23)
	public String getClientId() {
		return clientId;
	}

	@ModuleOption("identifies the client")
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getUrl() {
		return url;
	}

	@ModuleOption("location of the mqtt broker")
	public void setUrl(String url) {
		this.url = url;
	}

	public String getTopics() {
		return topics;
	}

	@ModuleOption("the topic(s) to which the source will subscribe")
	public void setTopics(String topics) {
		this.topics = topics;
	}

	public String getUsername() {
		return username;
	}

	@ModuleOption("the username to use when connecting to the broker")
	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	@ModuleOption("the password to use when connecting to the broker")
	public void setPassword(String password) {
		this.password = password;
	}

}
