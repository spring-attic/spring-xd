/*
 * Copyright 2013-2015 the original author or authors.
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

import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;

import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Describes options to the {@code mqtt} sink module.
 * 
 * @author Eric Bottard
 * @author Gary Russell
 */
@Mixin(MqttConnectionMixin.class)
public class MqttSinkOptionsMetadata {

	private String clientId = "xd.mqtt.client.id.snk";

	private String topic = "xd.mqtt.test";

	private int qos = 1;

	private boolean retained = false;

	private String charset = "UTF-8";

	private boolean async = false;


	@Range(min = 0, max = 2)
	public int getQos() {
		return qos;
	}


	public boolean isRetained() {
		return retained;
	}

	@ModuleOption("the quality of service to use")
	public void setQos(int qos) {
		this.qos = qos;
	}

	@ModuleOption("whether to set the 'retained' flag")
	public void setRetained(boolean retained) {
		this.retained = retained;
	}

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
	public String getTopic() {
		return topic;
	}

	@ModuleOption("the topic to which the sink will publish")
	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getCharset() {
		return charset;
	}

	@ModuleOption("the charset used to convert a String payload to byte[]")
	public void setCharset(String charset) {
		this.charset = charset;
	}

	public boolean isAsync() {
		return async;
	}

	@ModuleOption("whether or not to use async sends")
	public void setAsync(boolean async) {
		this.async = async;
	}

}
