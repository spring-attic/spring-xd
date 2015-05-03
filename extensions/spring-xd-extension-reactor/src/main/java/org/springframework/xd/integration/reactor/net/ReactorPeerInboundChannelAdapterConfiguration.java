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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;
import reactor.Environment;
import reactor.core.dispatch.SynchronousDispatcher;
import reactor.io.codec.Codec;
import reactor.io.net.ChannelStream;
import reactor.io.net.ReactorPeer;

import java.util.Map;

/**
 * @author Stephane Maldini
 */
@Configuration
@SuppressWarnings({"rawtypes"})
public class ReactorPeerInboundChannelAdapterConfiguration<IN, OUT> {

	@Autowired(required = false)
	protected Map<String, Codec> codecs;

	@Value("${dispatcher:}")
	protected String dispatcher;

	@Value("${protocol:tcp}")
	protected String protocol;

	@Value("${host:}")
	protected String host;

	@Value("${port:-1}")
	protected int port;

	@Value("${framing:}")
	protected String framing;

	@Value("${lengthFieldLength:-1}")
	protected int lengthFieldLength;

	@Value("${codec:}")
	protected String codec;

	@Autowired
	@Qualifier("output")
	protected MessageChannel outputChannel;

	@Autowired(required = false)
	@Qualifier("errorChannel")
	protected MessageChannel errorChannel;

	@Autowired(required = false)
	private Environment env;

	@Bean
	public ReactorPeerFactoryBean<IN, OUT> netServerSpecFactoryBean() {
		ReactorPeerFactoryBean<IN, OUT> peerFactoryBean = new ReactorPeerFactoryBean<IN, OUT>(env, protocol, codecs);

		if (!framing.isEmpty()) {
			peerFactoryBean.setFraming(framing);
		}

		if (lengthFieldLength != -1) {
			peerFactoryBean.setLengthFieldLength(lengthFieldLength);
		}

		if (!codec.isEmpty()) {
			peerFactoryBean.setCodec(codec);
		}

		if (port != -1) {
			peerFactoryBean.setPort(port);
		}

		if (!host.isEmpty()) {
			peerFactoryBean.setHost(host);
		}

		if (!dispatcher.isEmpty()) {
			return peerFactoryBean.setDispatcher(dispatcher);
		} else {
			return peerFactoryBean.setDispatcher(SynchronousDispatcher.INSTANCE);
		}
	}

	@Bean
	public ReactorPeerInboundChannelAdapter<IN, OUT> netServerInboundChannelAdapter(
			ReactorPeer<IN, OUT, ChannelStream<IN, OUT>> peer
	) {
		ReactorPeerInboundChannelAdapter<IN, OUT> adapter = new ReactorPeerInboundChannelAdapter<IN, OUT>(peer);
		adapter.setOutputChannel(outputChannel);
		adapter.setErrorChannel(errorChannel);
		return adapter;
	}

	public void setCodecs(Map<String, Codec> codecs) {
		this.codecs = codecs;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setDispatcher(String dispatcher) {
		this.dispatcher = dispatcher;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setFraming(String framing) {
		this.framing = framing;
	}

	public void setLengthFieldLength(int lengthFieldLength) {
		this.lengthFieldLength = lengthFieldLength;
	}

	public void setCodec(String codec) {
		this.codec = codec;
	}

	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	public void setErrorChannel(MessageChannel errorChannel) {
		this.errorChannel = errorChannel;
	}

	public void setEnv(Environment env) {
		this.env = env;
	}
}
