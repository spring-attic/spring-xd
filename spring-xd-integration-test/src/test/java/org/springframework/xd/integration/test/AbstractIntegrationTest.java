/*
 * Copyright 2011-2014 the original author or authors.
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

package org.springframework.xd.integration.test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.xd.integration.util.Sink;
import org.springframework.xd.integration.util.StreamUtils;
import org.springframework.xd.integration.util.XdEc2Validation;
import org.springframework.xd.integration.util.XdEnvironment;

/**
 * Base Class for Spring XD Integration classes
 * 
 * @author Glenn Renfro
 */
public abstract class AbstractIntegrationTest {

	private final static String STREAM_NAME = "ec2Test3";

	private final static String HTTP_PREFIX = "http://";

	protected static XdEnvironment hosts;

	protected static XdEc2Validation validation;

	protected static URL adminServer;

	protected static List<URL> containers;

	protected static int jmxPort;

	protected static int httpPort;

	protected List<String> streamNames;

	protected static String privateKey;

	protected static String containerLogLocation;

	protected Sink sink;

	protected static String XD_DELIMETER = " | ";


	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		hosts = new XdEnvironment();
		adminServer = hosts.getAdminServer();
		containers = hosts.getContainers();
		validation = new XdEc2Validation();
		validation.verifyXDAdminReady(adminServer);
		jmxPort = hosts.getJMXPort();
		httpPort = hosts.getHttpPort();
		privateKey = hosts.getPrivateKey();
		containerLogLocation = hosts.getContainerLogLocation();
		validation.verifyAtLeastOneContainerAvailable(hosts.getContainers(),
				jmxPort);

	}

	@Before
	public void setup() {
		streamNames = new ArrayList<String>();
	}

	@After
	public void tearDown() throws IOException, URISyntaxException {
		StreamUtils.destroyAllStreams(streamNames, adminServer);
		waitForXD();
	}

	@Parameters
	public static Collection<Object[]> sink() {
		Object[][] sink = { { Sink.FILE }, { Sink.LOG } };
		return Arrays.asList(sink);
	}


	/**
	 * Creates a stream on the XD cluster defined by the test's Artifact or Environment variables
	 * 
	 * @param stream the stream definition
	 * @throws IOException
	 */
	public void stream(String stream) throws IOException, URISyntaxException {
		StreamUtils.stream(STREAM_NAME, stream, adminServer);
		streamNames.add(STREAM_NAME);
		waitForXD();
	}

	public boolean send(String type, String message) throws IOException {
		boolean result = true;
		waitForXD(2000);// Extended wait time was need for the ProcessorTests.
		if (type.equalsIgnoreCase(StreamUtils.SendTypes.HTTP.name())) {
			URL originURL = getContainerForStream(STREAM_NAME);
			URL targetURL = new URL(HTTP_PREFIX + originURL.getHost() + ":"
					+ httpPort);
			StreamUtils.send(StreamUtils.SendTypes.HTTP, message, targetURL);
		}
		waitForXD();
		return result;
	}

	public URL getContainerForStream(String streamName) {
		// Assuming one container for now.
		return containers.get(0);
	}

	public int getJMXPort() {
		// Assuming one containerJMXPort for now.
		return jmxPort;
	}

	public void assertReceived() throws Exception {
		waitForXD();// need this wait in case the send takes too long or Stream
					// takes too long to build

		validation.assertReceived(hosts, StreamUtils.replacePort(
				getContainerForStream(STREAM_NAME), jmxPort), STREAM_NAME,
				"http");
	}

	public void assertValid(String data) throws IOException {
		if (sink.equals(Sink.FILE)) {
			assertValidFile(data, getContainerForStream(STREAM_NAME), STREAM_NAME);
		}
		if (sink.equals(Sink.LOG)) {
			assertLogEntry(data, getContainerForStream(STREAM_NAME));
		}
	}

	public void assertValidFile(String data, URL url, String streamName)
			throws IOException {
		waitForXD();
		String fileName = XdEnvironment.RESULT_LOCATION + "/" + streamName
				+ ".out";
		validation.verifyTestContent(hosts, url, fileName, data);
	}

	public void assertLogEntry(String data, URL url)
			throws IOException {
		waitForXD();
		validation.verifyLogContent(hosts, url, containerLogLocation, data);
	}

	private void waitForXD() {
		waitForXD(1000);
	}

	private void waitForXD(int millis) {
		try {
			Thread.sleep(millis);
		}
		catch (Exception ex) {
			// ignore
		}

	}
}
