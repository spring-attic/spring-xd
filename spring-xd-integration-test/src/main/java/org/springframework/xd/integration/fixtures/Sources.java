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

import java.net.URL;
import java.util.List;

import org.springframework.xd.test.fixtures.JmsSource;
import org.springframework.xd.test.fixtures.MqttSource;
import org.springframework.xd.test.fixtures.SimpleFileSource;
import org.springframework.xd.test.fixtures.SimpleHttpSource;
import org.springframework.xd.test.fixtures.SimpleTailSource;
import org.springframework.xd.test.fixtures.TcpSource;


/**
 * 
 * @author Glenn Renfro
 */
public class Sources {

	private URL adminServer = null;

	private List<URL> containers = null;

	private SimpleHttpSource httpSource = null;

	private TcpSource tcpSource = null;

	private int httpPort = 9000;

	private int mqttPort = 1883;

	private String jmsHost;

	private int jmsPort;

	private final static int TCP_SINK_PORT = 1234;

	public Sources(URL adminServer, List<URL> containers, int httpPort, String jmsHost,
			int jmsPort) {
		this.adminServer = adminServer;
		this.containers = containers;
		this.httpPort = httpPort;
		this.jmsHost = jmsHost;
		this.jmsPort = jmsPort;
	}

	public SimpleHttpSource http() throws Exception {
		if (httpSource == null) {
			httpSource = http(httpPort);
		}
		return httpSource;
	}

	public SimpleHttpSource http(int port) throws Exception {
		return new SimpleHttpSource(containers.get(0).getHost(), port);
	}

	public TcpSource tcp() {
		if (tcpSource == null) {
			tcpSource = tcp(TCP_SINK_PORT);
		}
		return tcpSource;
	}

	public TcpSource tcp(int port) {
		return new TcpSource(containers.get(0).getHost(), port);
	}

	public SimpleTailSource tail(int delay, String fileName) throws Exception {
		return new SimpleTailSource(delay, fileName);
	}

	public JmsSource jms() {
		return new JmsSource(jmsHost, jmsPort);
	}

	public MqttSource mqtt() {
		return new MqttSource(adminServer.getHost(), mqttPort);
	}

	public SimpleFileSource file(String dir, String fileName) throws Exception {
		return new SimpleFileSource(dir, fileName);
	}

	public String jmsConfig() {
		String result = "amq.url=tcp://" + jmsHost + ":" + jmsPort;
		return result;
	}

}
