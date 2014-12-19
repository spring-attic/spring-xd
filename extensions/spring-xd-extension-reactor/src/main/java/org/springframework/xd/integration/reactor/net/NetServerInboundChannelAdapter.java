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

import reactor.fn.Consumer;
import reactor.net.NetServer;
import reactor.net.spec.NetServerSpec;

import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * Inbound ChannelAdapter that uses Reactor's {@code NetServer} abstraction to provide high-speed TCP or UDP ingest.
 *
 * @author Jon Brisbin
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class NetServerInboundChannelAdapter extends MessageProducerSupport {

	private final NetServerSpec spec;
	private volatile NetServer server;

	public NetServerInboundChannelAdapter(NetServerSpec spec) {
		Assert.notNull(spec, "NetServerSpec cannot be null");
		this.spec = spec;
	}

	@Override
	public String getComponentType() {
		return "int-reactor:netserver-inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.server = (NetServer)this.spec.consumeInput(new Consumer() {
			@Override
			public void accept(Object o) {
				sendMessage(new GenericMessage<Object>(o));
			}
		}).get();
	}

	@Override
	protected void doStart() {
		try {
			Assert.notNull(server.start().await(), "Server did not start properly");
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	protected void doStop() {
		try {
			Assert.notNull(server.shutdown().await(), "Server did not shutdown properly");
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
