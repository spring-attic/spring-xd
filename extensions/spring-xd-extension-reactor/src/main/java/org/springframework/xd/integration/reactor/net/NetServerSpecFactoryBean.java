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
import java.util.HashMap;
import java.util.Map;

import reactor.core.Environment;
import reactor.event.dispatch.Dispatcher;
import reactor.io.encoding.Codec;
import reactor.io.encoding.DelimitedCodec;
import reactor.io.encoding.LengthFieldCodec;
import reactor.io.encoding.StandardCodecs;
import reactor.net.encoding.syslog.SyslogCodec;
import reactor.net.netty.tcp.NettyTcpServer;
import reactor.net.netty.udp.NettyDatagramServer;
import reactor.net.spec.NetServerSpec;
import reactor.net.tcp.spec.TcpServers;
import reactor.net.udp.spec.DatagramServers;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/**
 * @author Jon Brisbin
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class NetServerSpecFactoryBean implements FactoryBean<NetServerSpec> {

	private static final Map<String, Codec> DEFAULT_CODECS = new HashMap<String, Codec>();

	static {
		DEFAULT_CODECS.put("bytes", StandardCodecs.BYTE_ARRAY_CODEC);
		DEFAULT_CODECS.put("string", StandardCodecs.STRING_CODEC);
		DEFAULT_CODECS.put("syslog", new SyslogCodec());
	}

	private final Map<String, Codec> codecs;

	private final NetServerSpec spec;

	private volatile String host;

	private volatile int port;

	private volatile String framing;

	private volatile int lengthFieldLength;

	private volatile Codec delegateCodec;

	public NetServerSpecFactoryBean(Environment env) {
		this(env, "tcp", null);
	}

	public NetServerSpecFactoryBean(Environment env, String transport) {
		this(env, transport, null);
	}

	public NetServerSpecFactoryBean(Environment env, Map<String, Codec> codecs) {
		this(env, "tcp", codecs);
	}

	public NetServerSpecFactoryBean(Environment env, String transport, Map<String, Codec> codecs) {
		this.codecs = (null == codecs ? DEFAULT_CODECS : codecs);
		if ("tcp".equals(transport)) {
			spec = TcpServers.create(env, NettyTcpServer.class);
		}
		else if ("udp".equals(transport)) {
			spec = DatagramServers.create(env, NettyDatagramServer.class);
		}
		else {
			throw new IllegalArgumentException(transport + " not a value transport type");
		}
	}

	public NetServerSpecFactoryBean configure(URI uri) {
		setHost(null != uri.getHost() ? uri.getHost() : "0.0.0.0");
		setPort(uri.getPort() > 0 ? uri.getPort() : 3000);
		setFraming(null != uri.getPath() ? uri.getPath().substring(1) : "linefeed");
		this.delegateCodec = StandardCodecs.STRING_CODEC;

		if (null != uri.getQuery()) {
			String[] params = StringUtils.split(uri.getQuery(), "&");
			if (null == params) {
				params = new String[] {uri.getQuery()};
			}
			for (String pair : params) {
				String[] parts = StringUtils.split(pair, "=");
				if (parts.length > 1) {
					if ("codec".equals(parts[0])) {
						setCodec(parts[1]);
					}
					else if ("dispatcher".equals(parts[0])) {
						setDispatcher(parts[1]);
					}
					else if ("lengthFieldLength".equals(parts[0])) {
						setLengthFieldLength(Integer.parseInt(parts[1]));
					}
				}
			}
		}
		return this;
	}

	/**
	 * Set the name of the {@link reactor.event.dispatch.Dispatcher} to use, which will be pulled from the current {@link
	 * reactor.core.Environment}.
	 *
	 * @param dispatcher dispatcher name
	 * @return {@literal this}
	 */
	public NetServerSpecFactoryBean setDispatcher(String dispatcher) {
		spec.dispatcher(dispatcher);
		return this;
	}

	public NetServerSpecFactoryBean setDispatcher(Dispatcher dispatcher) {
		spec.dispatcher(dispatcher);
		return this;
	}

	/**
	 * Set the host to which this server will bind.
	 *
	 * @param host the host to bind to (defaults to {@code 0.0.0.0})
	 * @return {@literal this}
	 */
	public NetServerSpecFactoryBean setHost(String host) {
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
	public NetServerSpecFactoryBean setPort(int port) {
		Assert.isTrue(port > 0, "Port must be greater than 0");
		this.port = port;
		return this;
	}

	/**
	 * Set the type of {@link reactor.io.encoding.Codec} to use to managing encoding and decoding of the data. <p> The
	 * default options for codecs are: <ul> <li>{@code bytes} - Use the standard byte array codec.</li> <li>{@code
	 * string} - Use the standard String codec.</li> <li>{@code syslog} - Use the standard Syslog codec.</li> </ul>
	 * </p>
	 *
	 * @param codec the codec
	 * @return {@literal this}
	 */
	public NetServerSpecFactoryBean setCodec(String codec) {
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
	public NetServerSpecFactoryBean setFraming(String framing) {
		Assert.isTrue("linefeed".equals(framing) || "length".equals(framing));
		this.framing = framing;
		return this;
	}

	/**
	 * Set the length of the length field if using length-field framing.
	 *
	 * @param lengthFieldLength {@code 2} for a {@code short}, {@code 4} for an {@code int} (the default), or {@code 8}
	 * for a {@code long}
	 * @return {@literal this}
	 */
	public NetServerSpecFactoryBean setLengthFieldLength(int lengthFieldLength) {
		this.lengthFieldLength = lengthFieldLength;
		return this;
	}


	@Override
	public NetServerSpec getObject() throws Exception {
		spec.listen(host, port);

		if (null != framing) {
			if ("linefeed".equals(framing)) {
				spec.codec(new DelimitedCodec(delegateCodec));
			}
			else if ("length".equals(framing)) {
				spec.codec(new LengthFieldCodec(lengthFieldLength, delegateCodec));
			}
		}

		return spec;
	}

	@Override
	public Class<?> getObjectType() {
		return NetServerSpec.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
