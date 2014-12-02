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

package org.springframework.xd.test.fixtures;

/**
 * A test fixture that allows testing of the 'tcp-client' source module.
 *
 * @author Glenn Renfro
 */

public class TcpClientSource extends AbstractModuleFixture<TcpClientSource> {

	private static final String DEFAULT_OUTPUT_TYPE = "text/plain";

	private int port;

	private String host;

	private String outputType;


	public TcpClientSource(String host, int port) {
		this.host = host;
		this.port = port;
		outputType = DEFAULT_OUTPUT_TYPE;

	}

	@Override
	protected String toDSL() {
		return String.format("tcp-client --host=%s --port=%d --outputType=%s", host, port, outputType);
	}

	/**
	 * Sets the host for the fixture
	 *
	 * @param host The host where the tcp-client will request data
	 * @return current instance of fixture
	 */
	public TcpClientSource host(String host) {
		this.host = host;
		return this;
	}

	/**
	 * Sets the port for the fixture
	 *
	 * @param port The port where tcp-client will request data
	 * @return current instance of fixture
	 */
	public TcpClientSource port(int port) {
		this.port = port;
		return this;
	}

	/**
	 * Sets the output type for the fixture
	 *
	 * @param outputType The conversion type for tcp-client
	 * @return current instance of fixture
	 */
	public TcpClientSource outputType(String outputType) {
		this.outputType = outputType;
		return this;
	}

}
