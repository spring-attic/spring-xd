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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.xd.integration.util.StreamUtils;
import org.springframework.xd.integration.util.XdEc2Hosts;
import org.springframework.xd.integration.util.XdEc2Validation;

/**
 * Base Class for Spring XD Integration classes
 * 
 * @author Glenn Renfro
 */
public abstract class AbstractIntegrationTest {

	private final static String STREAM_NAME = "ec2Test3";
	private final static String HTTP_PREFIX = "http://";

	protected static XdEc2Hosts hosts;
	protected static XdEc2Validation validation;
	protected static URL adminServer;
	protected static List<URL> containers;
	protected static int jmxPort;
	protected static int httpPort;
	protected List<String> streamNames;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		hosts = new XdEc2Hosts();
		adminServer = hosts.getAdminServer();
		containers = hosts.getContainers();
		validation = new XdEc2Validation();
		validation.verifyXDAdminReady(adminServer);
		jmxPort = hosts.getJMXPort();
		httpPort = hosts.getHttpPort();
		validation.verifyAtLeastOneContainerAvailable(hosts.getContainers(),jmxPort);

	}

	@Before
	public void setup() {
		streamNames = new ArrayList<String>();
	}

	@After
	public void tearDown() throws IOException {
		StreamUtils.destroyAllStreams(streamNames, adminServer);
		waitForXD(500);
	}

	/**
	 * Creates a stream on the XD cluster defined by the test's Artifact or
	 * Environment variables
	 * 
	 * @param stream
	 *            the stream definition
	 * @throws IOException
	 */
	public void stream(String stream) throws IOException {
		StreamUtils.stream(STREAM_NAME, stream, adminServer);
		streamNames.add(STREAM_NAME);
		waitForXD();
	}
	
	public boolean send(String type, String message) throws IOException {
		boolean result = true;
		waitForXD(1500);//Extended wait time was need for the ProcessorTests.  
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
		waitForXD();// need this wait in case the send takes too long or Stream takes too long to build
		
		validation.assertReceived(StreamUtils.replacePort(
				getContainerForStream(STREAM_NAME), jmxPort),
				STREAM_NAME, "http");
	}

	private void waitForXD() {
		waitForXD(1000);
	}
	private void waitForXD(int millis) {
		try {
			Thread.sleep(millis);
		} catch (Exception ex) {
			// ignore
		}

	}

}
