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

import org.springframework.integration.transformer.SyslogToMapTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.xd.integration.reactor.net.ReactorPeerInboundChannelAdapter;
import reactor.fn.Consumer;
import reactor.fn.Function;
import reactor.io.buffer.Buffer;
import reactor.io.net.ChannelStream;
import reactor.io.net.ReactorPeer;
import reactor.io.net.codec.syslog.SyslogCodec;
import reactor.io.net.codec.syslog.SyslogMessage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code InboundChannelAdapter} implementation that uses the Reactor TCP support to read in syslog messages and
 * transform them to a {@code Map} for use in downstream modules.
 *
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
public class SyslogInboundChannelAdapter extends ReactorPeerInboundChannelAdapter<Buffer, Buffer> {

	public SyslogInboundChannelAdapter(ReactorPeer<Buffer, Buffer, ChannelStream<Buffer, Buffer>> server)
	{
		super(server);
	}

	@Override
	public String getComponentType() {
		return "int-reactor:syslog-inbound-channel-adapter";
	}

	@Override
	protected void composeChannel(ChannelStream<Buffer, Buffer> input) {

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

		input.consume(new Consumer<Buffer>() {
			@Override
			public void accept(Buffer in) {
				decoder.apply(in);
			}
		});
	}
}
