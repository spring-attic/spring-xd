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

package org.springframework.integration.x.kafka;

import java.util.Properties;

import kafka.serializer.Decoder;
import kafka.utils.VerifiableProperties;


/**
 * String Decoder for Kafka message key/value decoding.
 *
 * @author Soby Chacko
 */
public class StringDecoder implements Decoder<String> {

	private String encoding = "UTF8";

	public void setEncoding(final String encoding) {
		this.encoding = encoding;
	}

	@Override
	public String fromBytes(byte[] bytes) {
		final Properties props = new Properties();
		props.put("serializer.encoding", encoding);

		final VerifiableProperties verifiableProperties = new VerifiableProperties(props);
		return new kafka.serializer.StringDecoder(verifiableProperties).fromBytes(bytes);
	}

}
