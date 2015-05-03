/*
 * Copyright 2013-2015 the original author or authors.
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

import org.reactivestreams.Publisher;
import reactor.fn.Consumer;
import reactor.io.net.ChannelStream;
import reactor.io.net.ReactorChannelHandler;
import reactor.io.net.ReactorPeer;

import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;
import reactor.rx.Promise;
import reactor.rx.Streams;

/**
 * Inbound ChannelAdapter that uses {@code ReactorPeer} abstraction to provide high-speed TCP or UDP ingest.
 *
 * @author Stephane Maldini
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ReactorPeerInboundChannelAdapter<IN, OUT> extends MessageProducerSupport {

	private final ReactorPeer<IN, OUT, ChannelStream<IN, OUT>> server;

	public ReactorPeerInboundChannelAdapter(ReactorPeer<IN, OUT, ChannelStream<IN, OUT>> reactorPeer) {
		Assert.notNull(reactorPeer, "ReactorPeer cannot be null");
		this.server = reactorPeer;
	}

	@Override
	public String getComponentType() {
		return "int-reactor:netserver-inbound-channel-adapter";
	}

	@Override
	protected void doStart() {
		try {
			Promise<Void> started = server.start(new ReactorChannelHandler<IN, OUT, ChannelStream<IN, OUT>>() {
				@Override
				public Publisher<Void> apply(ChannelStream<IN, OUT> input) {
					composeChannel(input);

					//never close
					return Streams.never();
				}
			});

			Assert.isTrue(started.awaitSuccess(), "Server did not start properly");
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	protected void doStop() {
		try {
			Assert.isTrue(server.shutdown().awaitSuccess(), "Server did not shutdown properly");
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	protected void composeChannel(ChannelStream<IN, OUT> input){
		input.consume(new Consumer<IN>() {
			@Override
			public void accept(IN in) {
				sendMessage(new GenericMessage<Object>(in));
			}
		});
	}

}
