/*
 * Copyright 2013-2014 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.xd.integration.reactor.net;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;
import reactor.Environment;
import reactor.io.codec.Codec;
import reactor.io.net.spec.NetServerSpec;

import java.util.Map;

/**
 * @author Jon Brisbin
 */
@Configuration
@SuppressWarnings({"rawtypes"})
public class NetServerInboundChannelAdapterConfiguration {

	@Autowired(required = false)
	private Map<String, Codec> codecs;

	@Value("${transport}")
	private String transport;

	@Value("${dispatcher}")
	private String dispatcher;

	@Value("${host}")
	private String host;

	@Value("${port}")
	private int port;

	@Value("${framing}")
	private String framing;

	@Value("${lengthFieldLength}")
	private int lengthFieldLength;

	@Value("${codec}")
	private String codec;

	@Bean
	public NetServerSpecFactoryBean netServerSpecFactoryBean(Environment env) {
		return new NetServerSpecFactoryBean(env, transport, codecs)
				.setDispatcher(dispatcher)
				.setFraming(framing)
				.setLengthFieldLength(lengthFieldLength)
				.setCodec(codec)
				.setPort(port)
				.setHost(host);
	}

	@Bean
	public NetServerInboundChannelAdapter netServerInboundChannelAdapter(NetServerSpec spec,
	                                                                     MessageChannel output) {
		NetServerInboundChannelAdapter adapter = new NetServerInboundChannelAdapter(spec);
		adapter.setOutputChannel(output);
		return adapter;
	}

	@Bean
	public Environment environment() {
		return Environment.initializeIfEmpty();
	}

}
