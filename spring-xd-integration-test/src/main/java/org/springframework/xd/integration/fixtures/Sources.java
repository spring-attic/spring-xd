/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.integration.fixtures;

import org.springframework.util.Assert;
import org.springframework.xd.integration.util.XdEnvironment;
import org.springframework.xd.test.fixtures.JmsSource;
import org.springframework.xd.test.fixtures.MqttSource;
import org.springframework.xd.test.fixtures.RabbitSource;
import org.springframework.xd.test.fixtures.SimpleFileSource;
import org.springframework.xd.test.fixtures.SimpleHttpSource;
import org.springframework.xd.test.fixtures.SimpleTailSource;
import org.springframework.xd.test.fixtures.TcpSource;
import org.springframework.xd.test.fixtures.TwitterSearchSource;


/**
 * A convenience class for creating instances of sources to be used for integration testing.
 *
 * Created with information about hosts and ports from the testing environment. Only supports one admin server and one
 * container location. The RabbitMQ broker is assumed to be at the same location as the admin server.
 *
 * @author Glenn Renfro
 * @author Mark Pollack
 */
public class Sources {

	private final XdEnvironment xdEnvironment;

	/**
	 * Construct a new Sources instance using the provided environment.
	 *
	 * @param xdEnvironment the environment with information on what port/hosts to connect to
	 */
	public Sources(XdEnvironment xdEnvironment) {
		Assert.notNull(xdEnvironment, "XDEnvironment can not be null");
		this.xdEnvironment = xdEnvironment;
	}

	/**
	 * Create an instance of the http source with the default target host and default port (9000).
	 *
	 * @return an instance of HttpSource
	 */
	public SimpleHttpSource http() {
		return new SimpleHttpSource(xdEnvironment.getDefaultTargetHost());
	}

	/**
	 * Create an instance of the http source with the default target host and provided port
	 *
	 * @param port the port to connect to
	 * @return an instance of HttpSource
	 */
	public SimpleHttpSource http(int port) {
		return new SimpleHttpSource(xdEnvironment.getDefaultTargetHost(), port);
	}

	/**
	 * Construct a new TcpSource with the default target host taken from the environment and default port (1234)
	 *
	 * @return an instance of TcpSource
	 */
	public TcpSource tcp() {
		return TcpSource.withDefaultPort(xdEnvironment.getDefaultTargetHost());
	}

	/**
	 * Construct a new TcpSource with the default target host taken from the environment and the provided port.
	 *
	 * @param port the port to connect to
	 * @return an instance of TcpSource
	 */
	public TcpSource tcp(int port) {
		return new TcpSource(xdEnvironment.getDefaultTargetHost(), port);
	}

	/**
	 * Construct a new SimpleTailSource with the the provided file name and delay
	 *
	 * @param delayInMillis on platforms that don't wait for a missing file to appear, how often (ms) to look for the
	 *        file.
	 * @param fileName the absolute path of the file to tail
	 * @return a tail source
	 */
	public SimpleTailSource tail(int delayInMillis, String fileName) {
		return new SimpleTailSource(delayInMillis, fileName);
	}

	/**
	 * Construct a new JmsSource using the default JMS Broker host and port as specified in the environment
	 *
	 * @return a jms source
	 */
	public JmsSource jms() {
		return new JmsSource(xdEnvironment.getJmsHost(), xdEnvironment.getJmsPort());
	}

	/**
	 * Construct a new MqttSource using the default RabbitMQ (MQTT-enbaled) broker host as specified in the environment.
	 *
	 * @return a mqtt source
	 */
	public MqttSource mqtt() {
		return new MqttSource(xdEnvironment.getRabbitMQHost());
	}

	/**
	 * Construct a new SimpleFileSource using the provided directory and filename
	 *
	 * @param dir directory name
	 * @param fileName file name
	 * @return new SimpleFileSource
	 */
	public SimpleFileSource file(String dir, String fileName) {
		Assert.notNull(dir, "dir should not be null");
		Assert.hasText(fileName, "fileName should not be empty nor null");
		return new SimpleFileSource(dir, fileName);
	}

	/**
	 * Construct a new RabbitSource using the environment and defaults.
	 *
	 * @return An instance of the rabbitsource fixture.
	 */
	public RabbitSource rabbitSource() {
		return RabbitSource.withDefaults(xdEnvironment.getMqConnectionFactory());
	}

	/**
	 * Construct a TwitterSearchSource using that will search for the query string provided..
	 *
	 * @param query The string to search for on twitter.
	 * @return An instance of the twitterSearchSource fixture.
	 */
	public TwitterSearchSource twitterSearch(String query) {
		Assert.hasText(query, "query must not be empty nor null");
		return TwitterSearchSource.withDefaults(xdEnvironment.getTwitterConsumerKey(),
				xdEnvironment.getTwitterConsumerSecretKey(), query);
	}

}
