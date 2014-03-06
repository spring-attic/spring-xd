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

import java.net.URI;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.MessageChannel;

import reactor.core.Environment;
import reactor.io.encoding.Codec;
import reactor.net.spec.NetServerSpec;
import reactor.spring.context.config.EnableReactor;

/**
 * @author Jon Brisbin
 */
@Configuration
@EnableReactor
@EnableIntegration
@SuppressWarnings({ "unchecked", "rawtypes" })
public class NetServerInboundChannelAdapterConfiguration {

	@Autowired(required = false)
	private Map<String, Codec> codecs;

	@Value("${bind}")
	private URI bindUri;

	@Bean
	public NetServerSpecFactoryBean netServerSpecFactoryBean(Environment env) {
		String transport = (null != bindUri.getScheme() ? bindUri.getScheme() : "tcp");
		return new NetServerSpecFactoryBean(env, transport, codecs).configure(bindUri);
	}

	@Bean
	public NetServerInboundChannelAdapter netServerInboundChannelAdapter(NetServerSpec spec,
			MessageChannel output) {
		NetServerInboundChannelAdapter adapter = new NetServerInboundChannelAdapter(spec);
		adapter.setOutputChannel(output);
		return adapter;
	}

}
