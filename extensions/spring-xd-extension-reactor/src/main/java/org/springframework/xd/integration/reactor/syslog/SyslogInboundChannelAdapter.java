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

import java.util.LinkedHashMap;
import java.util.Map;

import reactor.Environment;
import reactor.core.dispatch.SynchronousDispatcher;
import reactor.fn.Consumer;
import reactor.fn.Function;
import reactor.io.buffer.Buffer;
import reactor.io.codec.DelimitedCodec;
import reactor.io.codec.StandardCodecs;
import reactor.io.net.NetChannel;
import reactor.io.net.NetServer;
import reactor.io.net.codec.syslog.SyslogCodec;
import reactor.io.net.codec.syslog.SyslogMessage;
import reactor.io.net.netty.tcp.NettyTcpServer;
import reactor.io.net.netty.udp.NettyDatagramServer;
import reactor.io.net.spec.NetServerSpec;
import reactor.io.net.tcp.spec.TcpServerSpec;
import reactor.io.net.udp.spec.DatagramServerSpec;

import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.transformer.SyslogToMapTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * {@code InboundChannelAdapter} implementation that uses the Reactor TCP support to read in syslog messages and
 * transform them to a {@code Map} for use in downstream modules.
 *
 * @author Jon Brisbin
 */
public class SyslogInboundChannelAdapter extends MessageProducerSupport {

	private final Environment env;

	private volatile NetServer<Buffer, Buffer> server;

	private volatile String transport = "tcp";

	private volatile String host = "0.0.0.0";

	private volatile int port = 5140;

	public SyslogInboundChannelAdapter(Environment env) {
		this.env = env;
	}

	/**
	 * Set the transport to use. Should be either 'tcp' or 'udp'.
	 *
	 * @param transport transport
	 */
	public void setTransport(String transport) {
		if (null == transport || (!"tcp".equals(transport.toLowerCase()) && !"udp".equals(transport.toLowerCase()))) {
			throw new IllegalArgumentException("Transport must be 'tcp' or 'udp'");
		}
		this.transport = transport.toLowerCase();
	}

	/**
	 * Set hostname to bind this server to.
	 *
	 * @param host hostname
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Set port to bind this server to.
	 *
	 * @param port port
	 */
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

		NetServerSpec<Buffer, Buffer, ? extends NetServerSpec<Buffer, Buffer, ?, ?>, ? extends NetServer<Buffer,
				Buffer>> spec;
		if ("udp".equals(transport)) {
			spec = new DatagramServerSpec<Buffer, Buffer>(NettyDatagramServer.class);
		}
		else {
			spec = new TcpServerSpec<Buffer, Buffer>(NettyTcpServer.class);
		}

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

						Message<Map<String, Object>> siMsg = new GenericMessage<Map<String, Object>>(m);
						sendMessage(siMsg);
					}
				});

		spec.env(env)
				// safest guess of Dispatcher since we don't know what's happening downstream
				.dispatcher(new SynchronousDispatcher())
						// optimize for massive throughput by using lightweight codec in server
				.codec(new DelimitedCodec<Buffer, Buffer>(false, StandardCodecs.PASS_THROUGH_CODEC))
				.listen(host, port)
				.consume(new Consumer<NetChannel<Buffer, Buffer>>() {
					@Override
					public void accept(NetChannel<Buffer, Buffer> conn) {
						// consume lines and delegate to codec
						conn.consume(new Consumer<Buffer>() {

							@Override
							public void accept(Buffer b) {
								decoder.apply(b);
							}
						});
					}
				});

		server = spec.get();
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
