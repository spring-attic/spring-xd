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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.SyslogToMapTransformer;
import org.springframework.messaging.Message;

import reactor.core.Environment;
import reactor.event.dispatch.SynchronousDispatcher;
import reactor.function.Consumer;
import reactor.function.Function;
import reactor.io.Buffer;
import reactor.tcp.TcpConnection;
import reactor.tcp.TcpServer;
import reactor.tcp.encoding.DelimitedCodec;
import reactor.tcp.encoding.StandardCodecs;
import reactor.tcp.encoding.syslog.SyslogCodec;
import reactor.tcp.encoding.syslog.SyslogMessage;
import reactor.tcp.netty.NettyTcpServer;
import reactor.tcp.spec.TcpServerSpec;

/**
 * {@code InboundChannelAdapter} implementation that uses the Reactor TCP support to read in syslog messages and
 * transform them to a {@code Map} for use in downstream modules.
 * 
 * @author Jon Brisbin
 */
public class SyslogInboundChannelAdapter extends MessageProducerSupport {

	private final TcpServerSpec<Buffer, Buffer> spec;

	private volatile TcpServer<Buffer, Buffer> server;

	private volatile String host = "0.0.0.0";

	private volatile int port = 5140;

	public SyslogInboundChannelAdapter(Environment env) {
		// this is faster than putting the codec directly on the server
		final Function<Buffer, SyslogMessage> decoder = new SyslogCodec()
				.decoder(new Consumer<SyslogMessage>() {

					@Override
					public void accept(SyslogMessage syslogMsg) {
						Map<String, Object> m = new LinkedHashMap<String, Object>();

						m.put("PRIORITY", syslogMsg.getPriority());
						m.put(SyslogToMapTransformer.FACILITY, syslogMsg.getFacility());
						m.put(SyslogToMapTransformer.SEVERITY, syslogMsg.getSeverity());
						m.put(SyslogToMapTransformer.TIMESTAMP, syslogMsg.getTimestamp());
						m.put(SyslogToMapTransformer.HOST, syslogMsg.getHost());
						m.put(SyslogToMapTransformer.MESSAGE, syslogMsg.getMessage());

						Message<Map<String, Object>> siMsg = MessageBuilder.withPayload(m).build();
						sendMessage(siMsg);
					}
				});

		this.spec = new TcpServerSpec<Buffer, Buffer>(NettyTcpServer.class)
				.env(env)
				// safest guess of Dispatcher since we don't know what's happening downstream
				.dispatcher(new SynchronousDispatcher())
				// optimize for massive throughput by using lightweight codec in server
				.codec(new DelimitedCodec<Buffer, Buffer>(false, StandardCodecs.PASS_THROUGH_CODEC))
				.consume(new Consumer<TcpConnection<Buffer, Buffer>>() {

					@Override
					public void accept(TcpConnection<Buffer, Buffer> conn) {
						// consume lines and delegate to codec
						conn.consume(new Consumer<Buffer>() {

							@Override
							public void accept(Buffer b) {
								decoder.apply(b);
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
