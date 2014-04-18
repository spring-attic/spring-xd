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

package org.springframework.xd.integration.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.xd.integration.util.jmxresult.JMXResult;
import org.springframework.xd.integration.util.jmxresult.Module;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Validates that all instances of the cluster is up and running. Also verifies that streams are running and available.
 * 
 * @author Glenn Renfro
 */
@Configuration
public class XdEc2Validation {

	private final transient RestTemplate restTemplate;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(XdEc2Validation.class);

	public XdEc2Validation() {
		restTemplate = new RestTemplate();
		((SimpleClientHttpRequestFactory) restTemplate.getRequestFactory())
				.setConnectTimeout(2000);
	}

	/**
	 * Checks to see if the admin server the user specified is available. Else it throws a ResourceAccessException.
	 * 
	 * @param adminServer
	 */
	public void verifyXDAdminReady(final URL adminServer) {
		boolean result = verifyAdminConnection(adminServer);
		assertTrue("XD Admin Server is not available at "
				+ adminServer.toString(), result);
	}

	/**
	 * * Checks to see if at least one server the user specified is available. Else it throws a ResourceAccessException.
	 * 
	 * @param containers
	 * @param jmxPort
	 * @throws Exception
	 */
	public void verifyAtLeastOneContainerAvailable(final List<URL> containers,
			int jmxPort) throws MalformedURLException {
		boolean result = false;
		final Iterator<URL> containerIter = containers.iterator();
		while (containerIter.hasNext()) {
			final URL container = containerIter.next();
			try {
				verifyContainerConnection(StreamUtils.replacePort(container,
						jmxPort));
				result = true;
			}
			catch (ResourceAccessException rae) {
				LOGGER.error("XD Container is not available at "
						+ StreamUtils.replacePort(container, jmxPort));
			}
			catch (HttpClientErrorException hcee) {
				LOGGER.debug(hcee.getMessage());
				result = true;
			}
			catch (IOException ioe) {
				LOGGER.warn("XD Container is not available at "
						+ StreamUtils.replacePort(container, jmxPort));
			}
		}
		assertTrue("No XD Containers are available", result);
	}

	/**
	 * Verifies that the module has in fact processed the data. Keep in mind that any module name must be suffixed with
	 * index number for example .1. So if I have a stream of http|file, to access the modules I will need to have a
	 * module name of http.1 for the source and file.1 for the sink.
	 * 
	 * @param url The server where the stream is deployed
	 * @param streamName The stream to analyze.
	 * @param moduleName The name of the module
	 * @throws Exception
	 */
	public void assertReceived(URL url, String streamName,
			String moduleName, int msgCountExpected) throws Exception {
		String request = buildJMXRequest(url, streamName, moduleName);
		List<Module> modules = getModuleList(StreamUtils.httpGet(new URL(
				request)));
		verifySendCounts(modules, msgCountExpected);
	}

	/**
	 * Retrieves the stream and verifies that all modules in the stream processed the data.
	 * 
	 * @param url The server where the stream is deployed
	 * @param streamName The stream to analyze.
	 * @throws Exception
	 */
	public void assertReceived(URL url, String streamName,
			int msgCountExpected) throws Exception {
		String request = buildJMXRequest(url, streamName, "*");
		List<Module> modules = getModuleList(StreamUtils.httpGet(new URL(
				request)));
		verifySendCounts(modules, msgCountExpected);
	}

	/**
	 * Verifies that the data user gave us is what was stored after the stream has processed the flow.
	 * 
	 * @param hosts Helper class for retrieving files.
	 * @param url The server that the stream is deployed.
	 * @param fileName The file that contains the data to check.
	 * @param data The data used to evaluate the results of the stream.
	 * @throws IOException
	 */
	public void verifyTestContent(XdEnvironment hosts, URL url, String fileName,
			String data) throws IOException {
		String resultFileName = fileName;
		if (hosts.isOnEc2()) {
			resultFileName = StreamUtils.transferResultsToLocal(hosts, url, fileName);
		}
		File file = new File(resultFileName);
		Reader fileReader = new InputStreamReader(new FileInputStream(resultFileName));
		try {
			String result = FileCopyUtils.copyToString(fileReader);

			assertEquals("Data in the result file is not what was sent. Read \""
					+ result + "\"\n but expected \"" + data + "\"", data, result);
		}
		finally {
			fileReader.close();
			if (file.exists()) {
				file.delete();
			}
		}

	}

	/**
	 * checks to see if the content of the data passed in is in the log.
	 * 
	 * @param hosts Helper class for retrieving log files
	 * @param url The URL of the server to where the stream is deployed.
	 * @param fileName The name of the log file to inspect
	 * @param data THe expected result.
	 * @throws IOException
	 */
	public void verifyLogContent(XdEnvironment hosts, URL url, String fileName,
			String data) throws IOException {
		String logLocation = fileName;

		if (hosts.isOnEc2()) {
			logLocation = StreamUtils.transferLogToTmp(hosts, url, fileName);
		}
		File file = new File(logLocation);
		try {

			if (!file.exists()) {
				throw new IllegalArgumentException(
						"The Log File for the container is not present.  Please be sure to set the "
								+ "xd_container_log_dir on your gradle build.");
			}
			BufferedReader fileReader = new BufferedReader(new FileReader(logLocation));
			boolean result = false;
			while (fileReader.ready())
			{
				String line = fileReader.readLine();
				if (line.contains(data)) {
					result = true;
					break;
				}
			}
			fileReader.close();
			assertTrue("Data in the result file is not what was sent. Read "
					+ result + "\n but expected " + data, result);

		}
		finally {
			if (file.exists()) {
				file.delete();
			}
		}
	}

	/**
	 * generates the JMX query string for getting module data.
	 * 
	 * @param url the container url where the stream is deployed
	 * @param streamName the name of the stream
	 * @param moduleName the module to evaluate on the stream
	 * @return
	 */
	private String buildJMXRequest(URL url, String streamName,
			String moduleName) {
		String result = url.toString() + "/jolokia/read/xd." + streamName
				+ ":module=" + moduleName + ",component=MessageChannel,name=*";
		return result;
	}

	private String buildJMXList(URL url) {
		String result = url.toString() + "/jolokia/list";
		return result;
	}

	/**
	 * retrieves a list of modules from the json result that was returned by Jolokia.
	 * 
	 * @param json
	 * @return
	 * @throws Exception
	 */
	private List<Module> getModuleList(String json) throws Exception {
		List<Module> result = null;
		ObjectMapper mapper = new ObjectMapper();
		JMXResult jmxResult = mapper.readValue(json,
				new TypeReference<JMXResult>() {
				});
		result = jmxResult.getValue().getModules();
		return result;
	}

	/**
	 * Makes sure that that at least one entry was processed by the modules in the stream. verifies that no errors
	 * occured.
	 * 
	 * @param modules THe list of modules to evaluate.
	 * @throws Exception
	 */
	private void verifySendCounts(List<Module> modules, int msgCountExpected) throws Exception {
		Iterator<Module> iter = modules.iterator();
		while (iter.hasNext()) {
			Module module = iter.next();
			if (!module.getModuleChannel().equals("output")
					&& !module.getModuleChannel().equals("input")) {
				continue;
			}
			int sendCount = Integer.parseInt(module.getSendCount());
			assertEquals("Module "
					+ module.getModuleName() + " for channel "
					+ module.getModuleChannel()
					+ " did not have expected count ", msgCountExpected, sendCount);
			int errorCount = Integer.parseInt(module.getSendErrorCount());
			assertFalse("Module "
					+ module.getModuleName() + " for channel "
					+ module.getModuleChannel() + " had an error count of "
					+ errorCount + ",  expected 0.", errorCount > 0);
		}
	}

	private boolean verifyAdminConnection(final URL host)
			throws ResourceAccessException {
		boolean result = true;
		try {
			restTemplate.getForObject(host.toString(), String.class);
		}
		catch (ResourceAccessException rae) {
			LOGGER.error("XD Admin Server is not available at "
					+ host.getHost());
			result = false;
		}
		return result;
	}

	private void verifyContainerConnection(final URL host) throws IOException {
		String request = buildJMXList(host);
		StreamUtils.httpGet(new URL(request));

	}

}
