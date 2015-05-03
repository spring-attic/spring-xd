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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.Environment;
import reactor.core.Dispatcher;
import reactor.io.buffer.Buffer;
import reactor.io.codec.Codec;
import reactor.io.codec.DelimitedCodec;
import reactor.io.codec.LengthFieldCodec;
import reactor.io.codec.StandardCodecs;
import reactor.io.net.ChannelStream;
import reactor.io.net.NetStreams;
import reactor.io.net.ReactorPeer;
import reactor.io.net.Spec;
import reactor.io.net.codec.syslog.SyslogCodec;
import reactor.io.net.impl.netty.http.NettyHttpServer;
import reactor.io.net.impl.netty.tcp.NettyTcpServer;
import reactor.io.net.impl.netty.udp.NettyDatagramServer;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Stephane Maldini
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ReactorPeerFactoryBean<IN, OUT> implements FactoryBean<ReactorPeer<IN, OUT, ?>> {

	private static final Map<String, Codec> DEFAULT_CODECS = new HashMap<String, Codec>();

	static final String UDP  = "udp";
	static final String TCP  = "tcp";

	static {
		DEFAULT_CODECS.put("bytes", StandardCodecs.BYTE_ARRAY_CODEC);
		DEFAULT_CODECS.put("string", StandardCodecs.STRING_CODEC);
		DEFAULT_CODECS.put("syslog", new SyslogCodec());
	}

	private final String transport;

	private final Environment environment;

	private final Map<String, Codec> codecs;

	private String host;

	private Dispatcher dispatcher;

	private int port;

	private String framing;

	private int lengthFieldLength;

	private Codec delegateCodec;


	public ReactorPeerFactoryBean(Environment env, String transport, Map<String, Codec> codecs) {
		this.codecs = (null == codecs ? DEFAULT_CODECS : codecs);
		this.transport = transport;
		this.environment = (null == env ? Environment.initializeIfEmpty().assignErrorJournal() : env);
	}

	/**
	 * Set the name of the {@link reactor.core.Dispatcher} to use, which will be pulled from the current {@link
	 * reactor.Environment}.
	 *
	 * @param dispatcher dispatcher name
	 * @return {@literal this}
	 */
	public ReactorPeerFactoryBean<IN, OUT> setDispatcher(String dispatcher) {
		Assert.notNull(environment, "Cannot lookup dispatcher from unassigned environment");
		this.dispatcher = environment.getDispatcher(dispatcher);
		return this;
	}

	public ReactorPeerFactoryBean<IN, OUT> setDispatcher(Dispatcher dispatcher) {
		this.dispatcher = dispatcher;
		return this;
	}

	/**
	 * Set the host to which this server will bind.
	 *
	 * @param host the host to bind to (defaults to {@code 0.0.0.0})
	 * @return {@literal this}
	 */
	public ReactorPeerFactoryBean<IN, OUT> setHost(String host) {
		Assert.notNull(host, "Host cannot be null.");
		this.host = host;
		return this;
	}

	/**
	 * Set the port to which this server will bind.
	 *
	 * @param port the port to bind to (defaults to {@code 3000})
	 * @return {@literal this}
	 */
	public ReactorPeerFactoryBean<IN, OUT> setPort(int port) {
		Assert.isTrue(port > 0, "Port must be greater than 0");
		this.port = port;
		return this;
	}

	/**
	 * Set the type of {@link reactor.io.codec.Codec} to use to managing encoding and decoding of the data. <p> The
	 * default options for codecs are: <ul> <li>{@code bytes} - Use the standard byte array codec.</li> <li>{@code
	 * string} - Use the standard String codec.</li> <li>{@code syslog} - Use the standard Syslog codec.</li> </ul>
	 * </p>
	 *
	 * @param codec the codec
	 * @return {@literal this}
	 */
	public ReactorPeerFactoryBean<IN, OUT> setCodec(String codec) {
		this.delegateCodec = codecs.get(codec);
		Assert.notNull(delegateCodec, "No Codec found for type " + codec);
		return this;
	}

	/**
	 * Set the type of framing to use. <p> The options for framing are: <ul> <li>{@code linefeed} - Means use an
	 * LF-delimited linefeed codec.</li> <li>{@code length} - Means use a length-field based codec where the initial
	 * bytes of a message are the length of the rest of the message.</li> </ul> </p>
	 *
	 * @param framing type of framing
	 * @return {@literal this}
	 */
	public ReactorPeerFactoryBean<IN, OUT> setFraming(String framing) {
		Assert.isTrue("linefeed".equals(framing) || "length".equals(framing));
		this.framing = framing;
		return this;
	}

	/**
	 * Set the length of the length field if using length-field framing.
	 *
	 * @param lengthFieldLength {@code 2} for a {@code short}, {@code 4} for an {@code int} (the default), or {@code 8}
	 *                          for a {@code long}
	 * @return {@literal this}
	 */
	public ReactorPeerFactoryBean<IN, OUT> setLengthFieldLength(int lengthFieldLength) {
		this.lengthFieldLength = lengthFieldLength;
		return this;
	}


	@Override
	public ReactorPeer<IN, OUT, ?> getObject() throws Exception {

		if (TCP.equals(transport)) {
			return NetStreams.tcpServer(NettyTcpServer.class, new NetStreams.TcpServerFactory<IN, OUT>() {
				@Override
				public Spec.TcpServerSpec<IN, OUT> apply(Spec.TcpServerSpec<IN, OUT> spec) {
					commonSpecProperties(spec);
					//Add specifics to TCP
					return spec;
				}
			});
		} else if (UDP.equals(transport)) {
			return NetStreams.udpServer(NettyDatagramServer.class, new NetStreams.UdpServerFactory<IN, OUT>() {
				@Override
				public Spec.DatagramServerSpec<IN, OUT> apply(Spec.DatagramServerSpec<IN, OUT> spec) {
					commonSpecProperties(spec);
					//Add specifics to UDP
					return spec;
				}
			});
		} else {
			throw new IllegalArgumentException(transport + " not a value transport protocol type, only udp or tcp allowed");
		}
	}

	@Override
	public Class<?> getObjectType() {
		return ReactorPeer.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	private void commonSpecProperties(Spec.PeerSpec<IN, OUT, ?, ?, ?> spec) {
		spec
				.env(environment)
				.dispatcher(dispatcher)
				.listen(host, port);
		if (null != framing) {
			if ("linefeed".equals(framing)) {
				if(delegateCodec != null) {
					spec.codec(new DelimitedCodec(delegateCodec));
				}else {
					// optimize for massive throughput by using lightweight codec in server
					spec.codec(new DelimitedCodec<>(false, (Codec<Buffer, IN, OUT>)StandardCodecs.PASS_THROUGH_CODEC));
				}
			} else if ("length".equals(framing)) {
				if(delegateCodec != null) {
					spec.codec(new LengthFieldCodec(lengthFieldLength, delegateCodec));
				}else{
					spec.codec(new LengthFieldCodec<IN, OUT>(lengthFieldLength, (Codec<Buffer, IN, OUT>)StandardCodecs.PASS_THROUGH_CODEC));
				}
			}
		} else if(delegateCodec != null){
			spec.codec(delegateCodec);
		}


	}

}
