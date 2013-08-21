/*
 * Copyright 2013 the original author or authors.
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

import org.springframework.integration.Message;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import reactor.core.Environment;
import reactor.event.dispatch.SynchronousDispatcher;
import reactor.function.Consumer;
import reactor.tcp.TcpConnection;
import reactor.tcp.TcpServer;
import reactor.tcp.encoding.syslog.SyslogCodec;
import reactor.tcp.encoding.syslog.SyslogMessage;
import reactor.tcp.netty.NettyTcpServer;
import reactor.tcp.spec.TcpServerSpec;

/**
 * @author Jon Brisbin
 */
public class SyslogInboundChannelAdapter extends MessageProducerSupport {

	private final TcpServerSpec<SyslogMessage, Void> spec;
	private volatile String host = "0.0.0.0";
	private volatile int    port = 5140;
	private volatile TcpServer<SyslogMessage, Void> server;

	public SyslogInboundChannelAdapter(Environment env) {
		this.spec = new TcpServerSpec<SyslogMessage, Void>(NettyTcpServer.class)
				.env(env)
				.dispatcher(new SynchronousDispatcher())
				.codec(new SyslogCodec())
				.consume(new Consumer<TcpConnection<SyslogMessage, Void>>() {
					@Override
					public void accept(TcpConnection<SyslogMessage, Void> conn) {
						conn.in().consume(new Consumer<SyslogMessage>() {
							@Override
							public void accept(SyslogMessage syslogMsg) {
								Message<SyslogMessage> siMsg = MessageBuilder.withPayload(syslogMsg).build();
								sendMessage(siMsg);
							}
						});
					}
				});
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String getComponentType() {
		return "int-reactor:syslog-inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();

		spec.listen(host, port);

		this.server = spec.get();
	}

	@Override
	protected void doStart() {
		server.start();
	}

	@Override
	protected void doStop() {
		server.shutdown();
	}

}
