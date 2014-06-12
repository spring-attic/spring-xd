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

package org.springframework.xd.test.fixtures;

import org.springframework.util.Assert;


/**
 * Test fixture that creates a mqtt sink.
 *
 * @author Glenn Renfro
 */
public class MqttSink extends AbstractModuleFixture<MqttSink> {

	protected int port = 1883;

	private String host;


	public MqttSink(String host, int port) {
		Assert.hasText(host, "host must not be empty nor null");
		this.host = host;
		this.port = port;
	}

	@Override
	protected String toDSL() {
		return "mqtt --url='tcp://" + host + ":" + port + "'";
	}


}
