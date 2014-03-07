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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import org.springframework.shell.Bootstrap;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.xd.integration.util.DistributedFileSink;
import org.springframework.xd.integration.util.Sink;
import org.springframework.xd.integration.util.Source;
import org.springframework.xd.integration.util.StreamUtils;
import org.springframework.xd.integration.util.XdEc2Validation;
import org.springframework.xd.integration.util.XdEnvironment;
import org.springframework.xd.shell.command.fixtures.AbstractModuleFixture;
import org.springframework.xd.shell.command.fixtures.LogSink;
import org.springframework.xd.test.RandomConfigurationSupport;

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

	protected static int pauseTime;

	protected static String containerLogLocation;

	protected static String XD_DELIMETER = " | ";

	private static JLineShellComponent shell;

	protected static Source source = null;

	protected static Sink sink = null;


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
		pauseTime = hosts.getPauseTime();
		validation.verifyAtLeastOneContainerAvailable(hosts.getContainers(),
				jmxPort);
		Bootstrap bootstrap = new Bootstrap(new String[] { "--port",
			RandomConfigurationSupport.getAdminServerPort() });
		shell = bootstrap.getJLineShellComponent();
		source = new Source(adminServer, containers, shell, httpPort);
		sink = new Sink(adminServer, containers, shell);

	}

	public static JLineShellComponent getShell() {
		return shell;
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		File file = new File(StreamUtils.TMP_DIR);
		if (file.exists()) {
			file.delete();
		}

	}

	@Before
	public void setup() throws IOException, URISyntaxException {
		StreamUtils.destroyAllStreams(streamNames, adminServer);
		waitForXD();
		streamNames = new ArrayList<String>();
	}

	@After
	public void tearDown() throws IOException, URISyntaxException {
		StreamUtils.destroyAllStreams(streamNames, adminServer);
		waitForXD();
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
		waitForXD(pauseTime * 2000);// Extended wait time was need for the ProcessorTests.
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

	public void assertValid(String data, AbstractModuleFixture sinkInstance) throws IOException {

		if (sinkInstance.getClass().equals(DistributedFileSink.class)) {
			assertValidFile(data, getContainerForStream(STREAM_NAME), STREAM_NAME);
		}
		if (sinkInstance.getClass().equals(LogSink.class)) {
			assertLogEntry(data, getContainerForStream(STREAM_NAME));
		}

	}

	public void assertValidFile(String data, URL url, String streamName)
			throws IOException {
		waitForXD(pauseTime * 2000);
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
		waitForXD(pauseTime * 1000);
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
