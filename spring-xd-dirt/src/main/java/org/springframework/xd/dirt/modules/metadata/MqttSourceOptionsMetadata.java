/*
 * Copyright 2013-2015 the original author or authors.
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

import org.hibernate.validator.constraints.NotBlank;

import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Describes options to the {@code mqtt} source module.
 * 
 * @author Eric Bottard
 * @author Gary Russell
 */
@Mixin(MqttConnectionMixin.class)
public class MqttSourceOptionsMetadata {

	private String clientId = "xd.mqtt.client.id.src";

	private String topics = "xd.mqtt.test";

	private String qos = "0";

	private boolean binary = false;

	private String charset = "UTF-8";


	@NotBlank
	@Size(min = 1, max = 23)
	public String getClientId() {
		return clientId;
	}

	@ModuleOption("identifies the client")
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	@NotBlank
	public String getTopics() {
		return topics;
	}

	@ModuleOption("the topic(s) (comma-delimited) to which the source will subscribe")
	public void setTopics(String topics) {
		this.topics = topics;
	}

	public String getQos() {
		return qos;
	}

	@ModuleOption("the qos; a single value for all topics or a comma-delimited list to match the topics")
	public void setQos(String qos) {
		this.qos = qos;
	}

	public String getCharset() {
		return charset;
	}

	@ModuleOption("the charset used to convert bytes to String (when binary is false)")
	public void setCharset(String charset) {
		this.charset = charset;
	}

	public boolean isBinary() {
		return binary;
	}

	@ModuleOption("true to leave the payload as bytes")
	public void setBinary(boolean binary) {
		this.binary = binary;
	}

}
