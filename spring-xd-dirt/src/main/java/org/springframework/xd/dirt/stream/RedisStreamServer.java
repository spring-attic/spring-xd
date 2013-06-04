/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.x.redis.RedisQueueOutboundChannelAdapter;
import org.springframework.xd.dirt.redis.ExceptionWrappingLettuceConnectionFactory;

/**
 * This is a temporary "server" for the REST API. Currently it only handles simple
 * stream configurations (tokens separated by pipes) without any parameters. This
 * will be completely replaced by a more robust solution. Intended for demo only.
 *
 * @author Mark Fisher
 * @author Jennifer Hickey
 * @author David Turanski
 */
public class RedisStreamServer extends StreamServer {

	public RedisStreamServer(StreamDeployer streamDeployer) {
		super(streamDeployer);
	}

	public static void main(String[] args) {
		try {
			bootstrap(args);
		}
		catch(RedisConnectionFailureException e) {
			final Log logger = LogFactory.getLog(RedisStreamServer.class);
			logger.fatal(e.getMessage());
			System.err.println("Redis does not seem to be running. Did you install and start Redis? " +
					"Please see the Getting Started section of the guide for instructions.");
			System.exit(1);
		}
	}

	private static void bootstrap(String[] args) {
		LettuceConnectionFactory connectionFactory = getConnectionFactory(args);
		connectionFactory.afterPropertiesSet();
		DirectChannel outputChannel = new DirectChannel();
		final RedisQueueOutboundChannelAdapter adapter = new RedisQueueOutboundChannelAdapter("queue.deployer", connectionFactory);
		adapter.setExtractPayload(false);
		adapter.afterPropertiesSet();
		outputChannel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				adapter.handleMessage(message);
			}
		});
		StreamDeployer streamDeployer = new DefaultStreamDeployer(outputChannel);
		StreamServer server = new RedisStreamServer(streamDeployer);
		server.afterPropertiesSet();
		server.start();
	}

	private static LettuceConnectionFactory getConnectionFactory(String[] args) {
		if (args.length >= 2) {
			return new ExceptionWrappingLettuceConnectionFactory(args[0], Integer.parseInt(args[1]));
		}
		else {
			return new ExceptionWrappingLettuceConnectionFactory();
		}
	}

}
