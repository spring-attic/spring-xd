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

package org.springframework.xd.integration.reactor.syslog;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.xd.integration.reactor.net.ReactorPeerFactoryBean;
import org.springframework.xd.integration.reactor.net.ReactorPeerInboundChannelAdapter;
import org.springframework.xd.integration.reactor.net.ReactorPeerInboundChannelAdapterConfiguration;
import reactor.io.buffer.Buffer;
import reactor.io.net.ChannelStream;
import reactor.io.net.ReactorPeer;

/**
 * @author Stephane Maldini
 */
@Configuration
public class SyslogInboundChannelAdapterConfiguration extends ReactorPeerInboundChannelAdapterConfiguration<Buffer, Buffer> {


	@Override
	public ReactorPeerFactoryBean<Buffer, Buffer> netServerSpecFactoryBean() {
		ReactorPeerFactoryBean<Buffer, Buffer> factoryBean = super.netServerSpecFactoryBean();
		if(port == -1){
			factoryBean.setPort(5140);
		}
		return factoryBean;
	}

	@Bean
	public ReactorPeerInboundChannelAdapter<Buffer, Buffer> netServerInboundChannelAdapter(
			ReactorPeer<Buffer, Buffer, ChannelStream<Buffer, Buffer>> peer
	) {
		ReactorPeerInboundChannelAdapter<Buffer, Buffer> adapter = new SyslogInboundChannelAdapter(peer);
		adapter.setOutputChannel(outputChannel);
		adapter.setErrorChannel(errorChannel);
		return adapter;
	}
}
