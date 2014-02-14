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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class XdEc2Validation {

	private final transient RestTemplate restTemplate;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(XdEc2Validation.class);

	public XdEc2Validation() {
		restTemplate = new RestTemplate();
		((SimpleClientHttpRequestFactory) restTemplate.getRequestFactory())
				.setConnectTimeout(2000);
	}

	public void verifyXDAdminReady(final URL adminServer) {
		try {
			verifyAdminConnection(adminServer);
		}
		catch (ResourceAccessException rae) {
			LOGGER.error("XD Admin Server is not available at "
					+ adminServer.toString());
			throw new ResourceAccessException(
					"XD Admin Server is not available at "
							+ adminServer.toString());
		}
	}

	public void verifyAtLeastOneContainerAvailable(final List<URL> containers,
			int jmxPort) throws Exception {
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
		if (!result) {
			throw new ResourceAccessException("No XD Containers are available");
		}
	}

	public void assertReceived(XdEnvironment hosts, URL url, String streamName,
			String moduleName) throws Exception {
		String request = buildJMXRequest(url, streamName, moduleName, 0);
		List<Module> modules = getModuleList(StreamUtils.httpGet(new URL(
				request)));
		verifySendCounts(modules);

	}

	public void verifyTestContent(XdEnvironment hosts, URL url, String fileName,
			String data) throws IOException {
		if (hosts.isOnEc2()) {
			StreamUtils.transferResultsToLocal(hosts, url, fileName);
		}
		Reader fileReader = new InputStreamReader(new FileInputStream(fileName));
		String result = FileCopyUtils.copyToString(fileReader);
		if (!result.equals(data + "\n")) {
			fileReader.close();
			throw new ResourceAccessException(
					"Data in the result file is not what was sent. Read "
							+ result + "\n but expected " + data);
		}

		fileReader.close();
	}


	public void verifyLogContent(XdEnvironment hosts, URL url, String fileName,
			String data) throws IOException {
		String logLocation = fileName;
		if (hosts.isOnEc2()) {
			logLocation = StreamUtils.transferLogToTmp(hosts, url, fileName);
		}
		File file = new File(logLocation);
		if (!file.exists()) {
			throw new IllegalArgumentException(
					"The Log File for the container is not present.  Please be sure to set the "
							+ XdEnvironment.XD_CONTAINER_LOG_DIR + " on your gradle build.");
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

		if (!result) {
			throw new ResourceAccessException(
					"Data in the result file is not what was sent. Read "
							+ result + "\n but expected " + data);
		}
	}


	private String buildJMXRequest(URL url, String streamName,
			String moduleName, int modulePosition) {
		String result = url.toString() + "/jolokia/read/xd." + streamName
				+ ":module=*,component=MessageChannel,name=*";
		return result;
	}

	private String buildJMXList(URL url) {
		String result = url.toString() + "/jolokia/list";
		return result;
	}

	private List<Module> getModuleList(String json) throws Exception {
		List<Module> result = null;
		ObjectMapper mapper = new ObjectMapper();
		JMXResult jmxResult = mapper.readValue(json,
				new TypeReference<JMXResult>() {
				});
		result = jmxResult.getValue().getModules();
		return result;
	}

	private void verifySendCounts(List<Module> modules) throws Exception {
		Iterator<Module> iter = modules.iterator();
		while (iter.hasNext()) {
			Module module = iter.next();
			if (!module.getModuleChannel().equals("output")
					&& !module.getModuleChannel().equals("input")) {
				continue;
			}
			int sendCount = Integer.parseInt(module.getSendCount());
			if (sendCount == 0) {
				throw new InvalidResultException("Module "
						+ module.getModuleName() + " for channel "
						+ module.getModuleChannel()
						+ " expected to have a send count  > 0");
			}
			int errorCount = Integer.parseInt(module.getSendErrorCount());
			if (errorCount > 0) {
				throw new InvalidResultException("Module "
						+ module.getModuleName() + " for channel "
						+ module.getModuleChannel() + " had an error count of "
						+ errorCount + ",  expected 0.");
			}
		}
	}

	private void verifyAdminConnection(final URL host)
			throws ResourceAccessException {
		restTemplate.getForObject(host.toString(), String.class);
	}

	private void verifyContainerConnection(final URL host) throws IOException {
		String request = buildJMXList(host);
		StreamUtils.httpGet(new URL(request));

	}

}
