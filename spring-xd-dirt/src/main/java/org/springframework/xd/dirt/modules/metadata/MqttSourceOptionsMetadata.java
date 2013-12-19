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

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.SourceModuleOptionsMetadataSupport;

/**
 * Describes options to the {@code mqtt} source module.
 * 
 * @author Eric Bottard
 */
@PropertySource(value = "${xd.config.home}/mqtt.properties", ignoreResourceNotFound = true)
public class MqttSourceOptionsMetadata extends SourceModuleOptionsMetadataSupport {

	private String clientId;

	private String url;

	private String topics;

	private String username;

	private String password;

	@NotNull
	@Size(min = 1, max = 23)
	public String getClientId() {
		return clientId;
	}

	@Value("${mqtt.default.client.id}.src")
	@ModuleOption("identifies the client")
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	@NotNull
	@NotBlank
	public String getUrl() {
		return url;
	}

	@Value("${mqtt.url}")
	@ModuleOption("location of the mqtt broker")
	public void setUrl(String url) {
		this.url = url;
	}

	@NotBlank
	public String getTopics() {
		return topics;
	}

	@Value("${mqtt.default.topics}")
	@ModuleOption("the topic(s) to which the source will subscribe")
	public void setTopics(String topics) {
		this.topics = topics;
	}

	public String getUsername() {
		return username;
	}

	@Value("${mqtt.username}")
	@ModuleOption("the username to use when connecting to the broker")
	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	@Value("${mqtt.password}")
	@ModuleOption("the password to use when connecting to the broker")
	public void setPassword(String password) {
		this.password = password;
	}

}
