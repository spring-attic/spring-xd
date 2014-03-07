/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.integration.util;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.shell.core.JLineShellComponent;
import org.springframework.xd.integration.fixtures.FileSink;
import org.springframework.xd.shell.command.fixtures.AbstractModuleFixture;
import org.springframework.xd.shell.command.fixtures.HttpSource;
import org.springframework.xd.shell.command.fixtures.LogSink;
import org.springframework.xd.shell.command.fixtures.TcpSink;


/**
 * 
 * @author renfrg
 */
public class Sink {

	private static int TCP_SINK_PORT = 1234;

	private URL adminServer = null;

	private List<URL> containers = null;

	private JLineShellComponent shell = null;

	private HttpSource httpSource = null;

	private Map<String, AbstractModuleFixture> sinks;

	private int httpPort;

	private TcpSink tcpSink = null;

	public Sink(URL adminServer, List<URL> containers, JLineShellComponent shell, int httpPort) {
		this.adminServer = adminServer;
		this.containers = containers;
		this.shell = shell;
		this.httpPort = httpPort;
		sinks = new HashMap<String, AbstractModuleFixture>();
	}

	public AbstractModuleFixture getSink(Class clazz) {
		AbstractModuleFixture result = null;
		result = sinks.get(clazz.getName());
		if (result == null) {
			result = generateFixture(clazz.getName());
			sinks.put(clazz.getName(), result);
		}
		return result;
	}

	private AbstractModuleFixture generateFixture(String clazzName) {
		AbstractModuleFixture result = null;
		if (clazzName.equals("org.springframework.xd.shell.command.fixtures.LogSink")) {
			result = new LogSink("logsink");
		}
		if (clazzName.equals("org.springframework.xd.integration.fixtures.FileSink")) {
			result = new FileSink();
		}
		if (clazzName.equals("org.springframework.xd.shell.command.fixtures.TcpSink")) {
			result = new TcpSink(httpPort);
		}
		return result;
	}

	public TcpSink tcp() {
		if (tcpSink == null) {
			tcpSink = tcp(TCP_SINK_PORT);
		}
		return tcpSink;
	}

	public TcpSink tcp(int port) {
		return new TcpSink(port);
	}

}
