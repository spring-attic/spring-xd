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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.xd.integration.util.jmxresult.JMXChannelResult;
import org.springframework.xd.integration.util.jmxresult.JMXResult;
import org.springframework.xd.integration.util.jmxresult.Module;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.*;

/**
 * Validates that all instances of the cluster is up and running. Also verifies that streams are running and available.
 *
 * @author Glenn Renfro
 */
@Configuration
public class XdEc2Validation {

	private static final Logger LOGGER = LoggerFactory.getLogger(XdEc2Validation.class);

	private final RestTemplate restTemplate;

	private HadoopUtils hadoopUtil;

	private XdEnvironment xdEnvironment;

	@Value("${xd_container_log_dir}")
	private String containerLogLocation;

	@Value("${xd_run_on_ec2:true}")
	private boolean isOnEc2;

	@Value("${xd_test_jps_command:jps}")
	private String jpsCommand;

	/**
	 * Construct a new instance of XdEc2Validation
	 */
	public XdEc2Validation(HadoopUtils hadoopUtil, XdEnvironment xdEnvironment) {
		Assert.notNull(hadoopUtil, "hadoopUtil should not be null");
		Assert.notNull(xdEnvironment, "xdEnvironment should not be null");
		restTemplate = new RestTemplate();
		((SimpleClientHttpRequestFactory) restTemplate.getRequestFactory())
				.setConnectTimeout(2000);
		this.xdEnvironment = xdEnvironment;
		this.hadoopUtil = hadoopUtil;
	}

	/**
	 * Assert is the admin server is available.
	 *
	 * @param adminServer the location of the admin server
	 */
	public void verifyXDAdminReady(final URL adminServer) {
		Assert.notNull(adminServer, "adminServer can not be null");
		boolean result = verifyAdminConnection(adminServer);
		assertTrue("XD Admin Server is not available at "
				+ adminServer.toString(), result);
	}

	/**
	 * Verifies that the instances for the channel and module has in fact processed the correct number of messages. Keep
	 * in mind that any module name must be suffixed with index number for example .1. So if I have a stream of
	 * http|file, to access the modules I will need to have a module name of http.1 for the source and file.1 for the
	 * sink.
	 *
	 * @param url The server where the stream is deployed
	 * @param streamName The stream to analyze.
	 * @param moduleName The name of the module.
	 * @param channelName The name of the channel to interrogate.
	 * @param msgCountExpected expected number of messages to have been successfully processed by the module.
	 */
	public void assertReceived(URL url, String streamName,
			String moduleName, String channelName, int msgCountExpected) {
		Assert.notNull(url, "The url should not be null");
		Assert.hasText(moduleName, "The modulName can not be empty nor null");
		Assert.hasText(streamName, "The streamName can not be empty nor null");
		Assert.hasText(channelName, "The channelName can not be empty nor null");
		String request = buildJMXRequest(url, streamName, moduleName, channelName);
		try {
			Module module = getModule(StreamUtils.httpGet(new URL(request)));
			assertEquals("Module "
					+ moduleName + " for channel " + channelName
					+ " did not have expected count ", msgCountExpected, Integer.parseInt(module.getSendCount()));

		}
		catch (IOException ioException) {
			throw new IllegalStateException(ioException.getMessage(), ioException);
		}

	}

	/**
	 * Retrieves the stream and verifies that all modules in the stream processed the data.
	 *
	 * @param url The server where the stream is deployed
	 * @param streamName The stream to analyze.
	 * @throws Exception Error processing JSON or making HTTP GET request
	 */
	public void assertReceived(URL url, String streamName,
			int msgCountExpected) {
		Assert.notNull(url, "The url should not be null");
		Assert.hasText(streamName, "The streamName can not be empty nor null");
		String request = buildJMXRequest(url, streamName, "*", "*");
		try {
			List<Module> modules = getModuleList(StreamUtils.httpGet(new URL(request)));
			verifySendCounts(modules, msgCountExpected);
		}
		catch (IOException ioException) {
			throw new IllegalStateException(ioException.getMessage(), ioException);
		}
	}

	/**
	 * Verifies that the data user gave us is what was stored after the stream has processed the flow.
	 *
	 * @param url The server that the stream is deployed.
	 * @param fileName The file that contains the data to check.
	 * @param data The data used to evaluate the results of the stream.
	 */
	public void verifyTestContent(URL url, String fileName,
			String data) {
		Assert.notNull(url, "url should not be null");
		Assert.hasText(fileName, "fileName can not be empty nor null");
		Assert.hasText(data, "data can not be empty nor null");
		String resultFileName = fileName;
		if (isOnEc2) {
			resultFileName = StreamUtils.transferResultsToLocal(xdEnvironment.getPrivateKey(), url, fileName);
		}
		File file = new File(resultFileName);
		try {
			Reader fileReader = new InputStreamReader(new FileInputStream(resultFileName));
			String result = FileCopyUtils.copyToString(fileReader);
			assertEquals("Data in the result file is not what was sent. Read \""
					+ result + "\"\n but expected \"" + data + "\"", data, result);
		}
		catch (IOException ioException) {
			throw new IllegalStateException(ioException.getMessage(), ioException);
		}
		finally {
			if (file.exists()) {
				file.delete();
			}
		}

	}

	/**
	 * Verifies that the data is contained in the container log.
	 * @param url The server where the container is deployed.
	 * @param data the value that will be searched for, within the log file.
	 */
	public void verifyLogContains(URL url, String data) {
		verifyContentContains(url, getContainerWithPid(url), data);
	}

	/**
	 * Verifies that the data user gave us is contained in the result.
	 * @param url The server where the container is deployed.
	 * @param fileName The file that contains the data to check.
	 * @param data The data used to evaluate the results of the stream.
	 */
	public void verifyContentContains(URL url, String fileName,
			String data) {
		Assert.notNull(url, "url can not be null");
		Assert.hasText(fileName, "fileName can not be empty nor null");
		Assert.hasText(data, "data can not be empty nor null");
		String result = getDataFromResultFile(url, fileName);
		assertTrue("Could not find data in result file.. Read \""
				+ result + "\"\n but didn't see \"" + data + "\"", result.contains(data));
	}

	/**
	 * Verifies that the data user gave us is contained in the result.
	 *
	 * @param url The server that the stream is deployed.
	 * @param fileName The file that contains the data to check.
	 * @param data The data used to evaluate the results of the stream.
	 */
	public void verifyContentContainsIgnoreCase(URL url, String fileName,
			String data) {
		Assert.notNull(url, "url can not be null");
		Assert.hasText(fileName, "fileName can not be empty nor null");
		Assert.hasText(data, "data can not be empty nor null");
		String result = getDataFromResultFile(url, fileName);
		assertTrue("Could not find data in result file.. Read \""
				+ result + "\"\n but didn't see \"" + data + "\"", result.toLowerCase().contains(data.toLowerCase()));
	}

	/**
	 * Evaluates the content of the hdfs file against a result.  If equal no action is taken else an assert is thrown.
	 * @param expectedResult The data that should be within the hdfs file.
	 * @param pathToHdfsFile The location of the file on the hdfs file system
	 */
	public void verifyHdfsTestContent(String expectedResult, String pathToHdfsFile) {
		Assert.hasText(pathToHdfsFile, "pathToHdfsFile must not be empty nor null");
		Assert.notNull(expectedResult, "pathToHdfsFile must not be null");

		assertTrue(pathToHdfsFile + " is not present on hdfs file system",
				hadoopUtil.waitForPath(10000, pathToHdfsFile));
		assertEquals("The data returned from hadoop was different than was sent.  ", expectedResult + "\n",
				hadoopUtil.getFileContentsFromHdfs(pathToHdfsFile));
	}


	/**
	 * Takes the content of the file and places it in a string. If the file is on EC2 it will copy the file from ec2 to
	 * the local machine.
	 *
	 * @param url The URL of the EC2 instance where the file is located. (if tests on ec2)
	 * @param fileName The name of the file that contains the data
	 * @return The content of the file as a string.
	 */
	private String getDataFromResultFile(URL url, String fileName) {
		String resultFileName = fileName;
		File file = new File(resultFileName);
		try {

			if (isOnEc2) {
				resultFileName = StreamUtils.transferResultsToLocal(xdEnvironment.getPrivateKey(), url, fileName);
				file = new File(resultFileName);
			}

			return FileCopyUtils.copyToString(new InputStreamReader(new FileInputStream(resultFileName)));
		}
		catch (IOException ioException) {
			throw new IllegalStateException(ioException.getMessage(), ioException);
		}
		finally {
			if (file.exists()) {
				file.delete();
			}
		}

	}

	/**
	 * Generates the Jolokia URL that will return module metrics data for a given stream, module, and 
	 * channel name.
	 *
	 * @param url the container url where the stream is deployed
	 * @param streamName the name of the stream
	 * @param moduleName the module to evaluate on the stream. Set it to * if you want all modules.
	 * @param channelName the channel to evaluate for the module.
	 * @return A URL to access module metrics data for the provided stream, module and channel name.
	 */
	private String buildJMXRequest(URL url, String streamName,
			String moduleName, String channelName) {
		String result = url.toString() + "/management/jolokia/read/xd." + streamName
				+ ":module=" + moduleName + ",component=MessageChannel,name=" + channelName;
		return result;
	}

	/**
	 * retrieves a list of modules from the json result that was returned by Jolokia.
	 *
	 * @param json raw json response string from jolokia
	 * @return A list of module information
	 * @throws Exception error parsing JSON
	 */
	private List<Module> getModuleList(String json) throws JsonMappingException, JsonParseException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JMXResult jmxResult = mapper.readValue(json,
				new TypeReference<JMXResult>() {
				});
		List<Module> result = jmxResult.getValue().getModules();
		return result;
	}

	/**
	 * Maps Jolokia's JSON return value for module metrics to a Java Module object.
	 *
	 * @param json raw json response string from jolokia
	 * @return A module metrics result
	 * @throws Exception error parsing JSON
	 */
	private Module getModule(String json) throws JsonMappingException, JsonParseException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JMXChannelResult jmxResult = mapper.readValue(json,
				new TypeReference<JMXChannelResult>() {
				});
		return jmxResult.getValue();
	}

	/**
	 * Asserts that the expected minimum number of messages were processed by the modules in the stream and that no errors
	 * occurred.
	 *
	 * @param modules The list of modules in the stream
	 * @param msgCountExpected The expected count
	 */
	private void verifySendCounts(List<Module> modules, int msgCountExpected) {
		verifySendCounts(modules, msgCountExpected, true);
	}

	/**
	 * Asserts that the expected number (or greater than or equal to the expected number) of messages were processed by
	 * the modules in the stream. Also asserts that no errors occurred.
	 *
	 * @param modules The list of modules to evaluate.
	 * @param msgCountExpected The expected count
	 * @param greaterThanOrEqualTo true if should use greaterThanOrEqualToComparison
	 */
	private void verifySendCounts(List<Module> modules, int msgCountExpected, boolean greaterThanOrEqualTo) {
		Iterator<Module> iter = modules.iterator();
		while (iter.hasNext()) {
			Module module = iter.next();
			if (!module.getModuleChannel().equals("output")
					&& !module.getModuleChannel().equals("input")) {
				continue;
			}
			int sendCount = Integer.parseInt(module.getSendCount());
			if (greaterThanOrEqualTo) {
				assertThat("Module " + module.getModuleName() + " for channel " + module.getModuleChannel() +
						" did not have at least expected count ",
						sendCount, greaterThanOrEqualTo(msgCountExpected));
			}
			else {
				assertEquals("Module "
						+ module.getModuleName() + " for channel "
						+ module.getModuleChannel()
						+ " did not have expected count ", msgCountExpected, sendCount);
			}
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

	private String getContainerWithPid(URL url) {
		String result = containerLogLocation;
		if (result.contains("[PID]")) {
			Integer[] pids = null;
			if (isOnEc2) {
				pids = StreamUtils.getContainerPidsFromURL(url, xdEnvironment.getPrivateKey(),
						jpsCommand);
			}
			else {
				pids = StreamUtils.getLocalContainerPids(jpsCommand);
			}
			//Supports one container per server or virtual instance.
			if (pids.length > 0) {
				String pid = pids[0].toString();
				result = StringUtils.replace(containerLogLocation, "[PID]", pid);
			}
		}
		return result;
	}


}
