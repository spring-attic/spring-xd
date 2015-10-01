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

import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jclouds.ec2.domain.RunningInstance;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.xd.integration.fixtures.*;
import org.springframework.xd.integration.util.ConfigUtil;
import org.springframework.xd.integration.util.HadoopUtils;
import org.springframework.xd.integration.util.StreamUtils;
import org.springframework.xd.integration.util.XdEc2Validation;
import org.springframework.xd.integration.util.XdEnvironment;
import org.springframework.xd.test.fixtures.AbstractModuleFixture;
import org.springframework.xd.test.fixtures.LogSink;
import org.springframework.xd.test.fixtures.SimpleFileSink;

/**
 * Base Class for Spring XD Integration test classes
 *
 * @author Glenn Renfro
 * @author David Turanski
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = IntegrationTestConfig.class)
public abstract class AbstractIntegrationTest {

	protected final static String STREAM_NAME = "ec2Test3";

	protected final static String XD_DELIMITER = " | ";

	public final static int WAIT_TIME = 10000;

	protected final static String XD_TAP_DELIMITER = " > ";


	@Autowired
	protected XdEnvironment xdEnvironment;

	@Autowired
	protected XdEc2Validation validation;

	protected URL adminServer;

	@Value("${xd_pause_time}")
	protected int pauseTime;

	@Value("${xd_run_on_ec2}")
	protected boolean isOnEc2;

	@Value("${aws_access_key:}")
	protected String awsAccessKey;

	@Value("${aws_secret_key:}")
	protected String awsSecretKey;

	@Value("${aws_region:}")
	protected String awsRegion;

	@Autowired
	protected Sources sources;

	@Autowired
	protected Sinks sinks;

	@Autowired
	protected Processors processors;

	@Autowired
	protected ConfigUtil configUtil;

	@Autowired
	protected HadoopUtils hadoopUtil;

	private boolean initialized = false;

	/**
	 * Maps the containerID to the container dns.
	 */
	private Map<String, String> containers;

	private ContainerResolver containerResolver;


	/**
	 * Initializes the environment before the test. Also asserts that the admin server is up and at least one container is
	 * available.
	 */
	public void initializer() {
		if (!initialized) {
			adminServer = xdEnvironment.getAdminServerUrl();
			validation.verifyXDAdminReady(adminServer);
			containers = getAvailableContainers(adminServer);
			containerResolver = new ContainerResolver(adminServer, containers, STREAM_NAME);
			sources.setContainerResolver(containerResolver);
			sinks.setContainerResolver(containerResolver);
			assertTrue("There must be at least one container", containers.size() > 0);
			initialized = true;
		}
	}

	/**
	 * Retrieves the containers that are recognized by the adminServer.
	 * If the test is on EC2 the IPs of the containers will be set to the external IPs versus the default internal IPs.
	 *
	 * @param adminServer The adminserver to interrogate.
	 * @return A Map of container servers , that is keyed on the containerID assigned by the admin server.
	 */
	private Map<String, String> getAvailableContainers(URL adminServer) {
		Map<String, String> result = null;
		result = StreamUtils.getAvailableContainers(adminServer);
		//if ec2 replace local aws DNS with external DNS
		if (isOnEc2) {
			Map<String, String> metadataIpMap = getPrivateIpToPublicIP(StreamUtils.getEC2RunningInstances(
					awsAccessKey, awsSecretKey, awsRegion));
			Iterator<String> keysIter = result.keySet().iterator();
			while (keysIter.hasNext()) {
				String key = keysIter.next();
				String privateIP = result.get(key);
				Iterator<String> metadataIter = metadataIpMap.keySet().iterator();
				while (metadataIter.hasNext()) {
					String metadataPrivateIP = metadataIter.next();
					//AWS metadata suffixes its data with .ec2.internal or .compute-1.internal.  So we are finding
					//the metadata private ip that contains the internal id returned by the admin server.
					if (metadataPrivateIP != null && metadataPrivateIP.contains(privateIP)) {
						result.put(key, metadataIpMap.get(metadataPrivateIP));
						break;
					}
				}
			}
		}
		return result;
	}

	private Map<String, String> getPrivateIpToPublicIP(List<RunningInstance> riList) {
		Map<String, String> privateIPMap = new HashMap<String, String>();
		Iterator<RunningInstance> runningInstanceIter = riList.iterator();
		while (runningInstanceIter.hasNext()) {
			RunningInstance ri = runningInstanceIter.next();
			privateIPMap.put(ri.getPrivateDnsName(), ri.getDnsName());
		}
		return privateIPMap;
	}

	/**
	 * Destroys the temporary directory.
	 */
	@AfterClass
	public static void tearDownAfterClass() {
		File file = new File(StreamUtils.TMP_DIR);
		if (file.exists()) {
			file.delete();
		}

	}

	/**
	 * Destroys all streams in the xd cluster and calls initializer.
	 */
	@Before
	public void setup() {
		initializer();
		StreamUtils.destroyAllStreams(adminServer);
		waitForXD();
	}

	/**
	 * Destroys all stream created in the test.
	 */
	@After
	public void tearDown() {
		StreamUtils.destroyAllStreams(adminServer);
		waitForXD();
	}

	/**
	 * Return the container resolver created by this test.
	 * @return container resolver
	 */
	public ContainerResolver getContainerResolver() {
		return containerResolver;
	}

	/**
	 * Creates a stream on the XD cluster defined by the test's Artifact or Environment variables Uses STREAM_NAME as
	 * default stream name.
	 *
	 * @param stream the stream definition
	 */
	public void stream(String stream) {
		stream(STREAM_NAME, stream);
	}

	/**
	 * Creates a stream on the XD cluster defined by the test's Artifact or Environment variables
	 *
	 * @param streamName the name of the stream
	 * @param stream the stream definition
	 */
	public void stream(String streamName, String stream) {
		Assert.hasText(streamName, "stream name can not be empty nor null");
		Assert.hasText(stream, "stream needs to be populated with a definition and can not be null");
		StreamUtils.stream(streamName, stream, adminServer);
		waitForXD();
		assertTrue("The stream did not deploy. ",
				waitForStreamDeployment(streamName, WAIT_TIME));
	}


	/**
	 * Creates a file in a source directory for file source base tests.
	 *
	 * @param sourceDir The directory to place the file
	 * @param fileName The name of the file where the data will be written
	 * @param data The data to be written to the file
	 */
	public void setupSourceDataFiles(String sourceDir, String fileName, String data) {
		setupDataFiles(containerResolver.getContainerHostForSource(), sourceDir, fileName, data);
	}

	/**
	 * Creates a file in a directory for file based tests.
	 *
	 * @param host The host machine that the data will be written
	 * @param sourceDir The directory to place the file
	 * @param fileName The name of the file where the data will be written
	 * @param data The data to be written to the file
	 * @result returns true if file was created else false
	 */
	public boolean setupDataFiles(String host, String sourceDir, String fileName, String data) {
		Assert.hasText(host, "host must not be empty nor null");
		Assert.hasText(fileName, "fileName must not be empty nor null");
		Assert.notNull(sourceDir, "sourceDir must not be null");
		Assert.notNull(data, "data must not be null");
		boolean result = false;
		if (xdEnvironment.isOnEc2()) {
			result = StreamUtils.createDataFileOnRemote(xdEnvironment.getPrivateKey(), host, sourceDir, fileName, data,
					WAIT_TIME);
		}
		else {
			try {
				File dirs = new File(sourceDir);
				dirs.mkdirs();
				dirs.deleteOnExit();
				File file = new File(sourceDir + "/" + fileName);
				file.deleteOnExit();
				file.createNewFile();
				FileCopyUtils.copy(data.getBytes(), file);
				result = file.exists();
			}
			catch (IOException ioe) {
				throw new IllegalStateException(ioe.getMessage(), ioe);
			}
		}
		return result;
	}

	/**
	 * Copies the files required for a job module to the admin server and its containers.
	 *
	 * @param moduleJobName The module's job name.
	 * @param jarFiles a list of jar File Objects to be copied to a job's lib directory
	 * @param configFiles a list of configuration files that need to be copied to the jobs config directory.
	 */

	protected void copyJobToCluster(String moduleJobName, List<File> jarFiles, List<File> configFiles) {
		Assert.hasText(moduleJobName, "moduleJobName must not be empty nor null");
		copyJobToHost(moduleJobName, jarFiles, configFiles, adminServer.getHost());
		Map<String, String> containerMap = getAvailableContainers(adminServer);
		for (String containerHost : containerMap.values()) {
			copyJobToHost(moduleJobName, jarFiles, configFiles, containerHost);
		}
	}

	/**
	 * Copies the files required for a job module to the host.
	 *
	 * @param moduleJobName The module's job name.
	 * @param jarFiles a list of jar File Objects to be copied to a job's lib directory
	 * @param configFiles a list of configuration files that need to be copied to the jobs config directory.
	 * @param host the IP where the files should be copied.  If isEc2 flag is false this param is ignored.
	 */
	private void copyJobToHost(String moduleJobName, List<File> jarFiles, List<File> configFiles, String host) {
		Iterator<File> iter = jarFiles.iterator();
		while (iter.hasNext()) {
			File jarFile = iter.next();
			Assert.isTrue(jarFile.exists(), jarFile.getAbsoluteFile() + " must exist");
		}
		iter = configFiles.iterator();
		while (iter.hasNext()) {
			File configFile = iter.next();
			Assert.isTrue(configFile.exists(), configFile.getAbsoluteFile() + " must exist");
		}

		URI jobDir = getBaseJobDirUri(moduleJobName);
		createJobDirectoryStructure(jobDir, host);
		try {
			URI baseUri = new URI(jobDir.getPath()
					+ "/lib/");
			copyFilesToTarget(baseUri, jarFiles, host);
			baseUri = new URI(jobDir.getPath()
					+ "/config/");
			copyFilesToTarget(baseUri, configFiles, host);
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException("Path to file is not properly formatted", e);
		}
	}

	/**
	 * Copies files to the specified target on the local machine or to a remote machine via
	 * ssh.
	 * @param baseUri Target location
	 * @param files The list of files to copy.
	 * @param host the host (if on remote machine) to target the file copy.
	 */
	private void copyFilesToTarget(URI baseUri, List<File> files, String host) {
		for (File file : files) {
			URI targetUri;
			try {
				targetUri = new URI(baseUri.getRawPath() + "/" + file.getName());
			}
			catch (URISyntaxException e1) {
				throw new IllegalStateException(baseUri.getRawPath() + "/" + file.getName() + " is not a valid URI", e1);
			}
			if (xdEnvironment.isOnEc2()) {
				StreamUtils.copyFileToRemote(xdEnvironment.getPrivateKey(), host, targetUri,
						file, WAIT_TIME);
			}
			else {
				try {
					FileCopyUtils.copy(file, new File(targetUri.getPath()));
				}
				catch (IOException e) {
					throw new IllegalStateException("copying job files to XD failed", e);
				}
			}
		}
	}

	/**
	 * Copies the files required for a test to the admin server and its containers.
	 * @param baseUri The directory to write the files.
	 * @param files The files to be copied to the cluster.
	 */
	protected void copyFileToCluster(URI baseUri, List<File> files) {
		Assert.notNull(baseUri, "baseUri must not be null");
		Assert.notNull(files, "files must not be null");
		Assert.isTrue(files.size()>0,
				"files must have at least 1 file enumerated in the list");
		copyFilesToTarget(baseUri,files, adminServer.getHost());
		Map<String, String> containerMap = getAvailableContainers(adminServer);
		for (String containerHost : containerMap.values()) {
			copyFilesToTarget(baseUri, files, containerHost);
		}
	}

	/**
	 * Creates the directory structure so that a job's module components can be installed properly.  This method extracts the root file name from the xmlFileName
	 * and uses this as the base name for the job module's directory.
	 *
	 * @param baseDir the base directory where a job module should be installed.
	 */
	private void createJobDirectoryStructure(URI baseDir, String host) {
		if (xdEnvironment.isOnEc2()) {
			Assert.isTrue(
					StreamUtils.createRemoteDirectory(baseDir.getPath(), host, xdEnvironment.getPrivateKey(), WAIT_TIME),
					"unable to create job module base directory for "+baseDir.getPath());
			Assert.isTrue(
					StreamUtils.createRemoteDirectory(baseDir.getPath() + "/config", host,
							xdEnvironment.getPrivateKey(),
							WAIT_TIME),
					"unable to create job module base directory for "+ baseDir.getPath() + "/config");
			Assert.isTrue(
					StreamUtils.createRemoteDirectory(baseDir.getPath() + "/lib", host, xdEnvironment.getPrivateKey(),
							WAIT_TIME),
					"unable to create job module base directory for "+ baseDir.getPath() + "/lib");
		}
		else {
			String path = baseDir.getPath();
			File file = new File(path);
			if (!file.exists()) {
				Assert.isTrue(file.mkdir(), "unable to create" + path);
			}
			path = baseDir.getPath() + "/lib";
			file = new File(path);
			if (!file.exists()) {
				Assert.isTrue(file.mkdir(), "unable to create" + path);
			}
			path = baseDir.getPath() + "/config";
			file = new File(path);
			if (!file.exists()) {
				Assert.isTrue(file.mkdir(), "unable to create" + path);
			}

		}

	}

	/**
	 * Constructs the job module directory name. .
	 *
	 * @param jobName The name of the xml configuration file.
	 * @return a URI path to the Job directory.
	 */
	private URI getBaseJobDirUri(String jobName) {
		URI result;
		try {
			result = new URI("file://" + xdEnvironment.getBaseDir() + "/modules/job/" + jobName + "/");
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException("base job directory is not properly formatted", e);
		}
		return result;
	}

	/**
	 * Appends data to the specified file wherever the source module for the stream is deployed.
	 *
	 * @param sourceDir The location of the file
	 * @param fileName The name of the file to be appended
	 * @param dataToAppend The data to be appended to the file
	 */
	public void appendDataToSourceTestFile(String sourceDir, String fileName, String dataToAppend) {
		Assert.hasText(fileName, "fileName must not be empty nor null");
		Assert.notNull(sourceDir, "sourceDir must not be null");
		Assert.notNull(dataToAppend, "dataToAppend must not be null");

		if (xdEnvironment.isOnEc2()) {
			StreamUtils.appendToRemoteFile(xdEnvironment.getPrivateKey(), containerResolver.getContainerHostForSource(), sourceDir,
					fileName,
					dataToAppend);
		}
		else {
			PrintWriter out = null;
			try {
				out = new PrintWriter(new BufferedWriter(new FileWriter(sourceDir + "/" + fileName, true)));
				out.println(dataToAppend);
				out.close();
			}
			catch (IOException ioe) {
				throw new IllegalStateException(ioe.getMessage(), ioe);
			}
			finally {
				if (out != null) {
					out.close();
				}
			}
		}
	}



	/**
	 * Asserts that the expected number of messages were received by all modules in a stream.
	 */
	public void assertReceived(int msgCountExpected) {
		waitForXD();

		validation.assertReceived(StreamUtils.replacePort(
						containerResolver.getContainerUrlForSink(STREAM_NAME), xdEnvironment.getJmxPort()),
				STREAM_NAME, msgCountExpected);
	}

	/**
	 * Asserts that all channels of the module channel combination, processed the correct number of messages
	 *
	 * @param containerUrl the container that is hosting the module
	 * @param moduleName the name of the module jmx element to interrogate.
	 * @param channelName the name of the channel jmx element to interrogate
	 * @param msgCountExpected The number of messages this module and channel should have sent.
	 */
	public void assertReceived(URL containerUrl, String moduleName, String channelName, int msgCountExpected) {
		waitForXD();

		validation.assertReceived(StreamUtils.replacePort(
						containerUrl, xdEnvironment.getJmxPort()),
				STREAM_NAME, moduleName, channelName, msgCountExpected);
	}

	/**
	 * Asserts that all channels of the processor channel combination, processed the correct number of messages
	 * The location of the processor is resolved at runtime.
	 *
	 * @param moduleName the name of the module jmx element to interrogate.
	 * @param channelName the name of the channel jmx element to interrogate
	 * @param msgCountExpected The number of messages this module and channel should have sent.
	 */
	public void assertReceivedByProcessor(String moduleName, String channelName, int msgCountExpected) {
		assertReceived(getContainerResolver().getContainerUrlForProcessor(), moduleName, channelName, msgCountExpected);
	}

	/**
	 * Asserts that the sink channel, processed the correct number of messages
	 * The location of the sink is resolved at runtime.
	 *
	 * @param moduleName the name of the module jmx element to interrogate.
	 * @param channelName the name of the channel jmx element to interrogate
	 * @param msgCountExpected The number of messages this module and channel should have sent.
	 */
	public void assertReceivedBySink(String moduleName, String channelName, int msgCountExpected) {
		assertReceived(getContainerResolver().getContainerUrlForSink(), moduleName, channelName, msgCountExpected);
	}

	/**
	 * Asserts that source channel, processed the correct number of messages
	 * The location of the source  is resolved at runtime.
	 *
	 * @param moduleName the name of the module jmx element to interrogate.
	 * @param channelName the name of the channel jmx element to interrogate
	 * @param msgCountExpected The number of messages this module and channel should have sent.
	 */
	public void assertReceivedBySource(String moduleName, String channelName, int msgCountExpected) {
		assertReceived(getContainerResolver().getContainerUrlForSource(), moduleName, channelName, msgCountExpected);
	}

	/**
	 * Asserts that the data stored by the file or log sink is what was expected.
	 *
	 * @param data The data expected in the file or log sink
	 * @param sinkInstance determines whether to look at the log or file for the result
	 */
	public void assertValid(String data, AbstractModuleFixture<?> sinkInstance) {
		Assert.hasText(data, "data can not be empty nor null");
		Assert.notNull(sinkInstance, "sinkInstance must not be null");
		if (sinkInstance.getClass().equals(SimpleFileSink.class)) {
			assertValidFile(data, containerResolver.getContainerUrlForSink(STREAM_NAME), STREAM_NAME);
		}
		if (sinkInstance.getClass().equals(LogSink.class)) {
			assertLogEntry(data, containerResolver.getContainerUrlForSink(STREAM_NAME));
		}

	}

	/**
	 * Asserts that the data stored by the file sink, whose name is based off the stream
	 * name, is what was expected.
	 *
	 * @param data The data expected in the file
	 */
	public void assertFileContains(String data) {
		assertFileContains(data, STREAM_NAME);
	}

	/**
	 * Asserts that the data stored by the file sink, whose name is based off the stream
	 * name, is what was expected.
	 *
	 * @param streamName the name of the stream that generated the file
	 * @param data The data expected in the file
	 */
	public void assertFileContains(String data, String streamName) {
		assertFileContains(data, containerResolver.getContainerUrlForSink(streamName), streamName);
	}

	/**
	 * Asserts that the data stored by a file sink, whose name is based off the stream name,
	 * is what was expected.  The assertion is case insensitive.
	 *
	 * @param data The data expected in the file
	 */
	public void assertFileContainsIgnoreCase(String data) {
		assertFileContainsIgnoreCase(data, STREAM_NAME);
	}
	
	/**
	 * Asserts that the data stored by a file sink, whose name is based off the stream name,
	 * is what was expected.  The assertion is case insensitive.
	 *
	 * @param streamName the name of the stream that generated the file
	 * @param data The data expected in the file
	 */
	public void assertFileContainsIgnoreCase(String data, String streamName) {
		assertFileContainsIgnoreCase(data, containerResolver.getContainerUrlForSink(streamName), streamName);
	}

	/**
	 * Undeploys the test stream
	 */
	public void undeployStream() {
		undeployStream(STREAM_NAME);
	}

	/**
	 * Undeploys the stream specified by the streamName
	 *
	 * @param streamName the name of the stream to undeploy.
	 */
	public void undeployStream(String streamName) {
		StreamUtils.undeployStream(adminServer, streamName);
	}

	/**
	 * Wait the "waitTime" for a stream to be deployed.
	 *
	 * @param waitTime the time in millis to wait.
	 * @return true if deployed else false.
	 */
	public boolean waitForStreamDeployment(int waitTime) {
		return waitForStreamDeployment(STREAM_NAME, waitTime);
	}

	/**
	 * Wait the "waitTime" for a stream to be deployed.
	 *
	 * @param streamName the name of stream to be evaluated.
	 * @param waitTime the time in millis to wait.
	 * @return true if deployed else false.
	 */
	public boolean waitForStreamDeployment(String streamName, int waitTime) {
		Assert.hasText(streamName, "streamName must not be empty nor null");
		return StreamUtils.waitForStreamDeployment(streamName, adminServer, waitTime);
	}

	/**
	 * Wait the "waitTime" for a metric to be created.
	 *
	 * @param name the name of metric to be evaluated.
	 * @return true if metric is created else false.
	 */
	public boolean waitForMetric(String name) {
		return StreamUtils.waitForMetric(name, adminServer, WAIT_TIME);
	}

	/**
	 * Retrieve count from the metric store for the counter name specified.
	 * @param name the counter's name
	 * @return long value associated with the name.
	 */
	public long getCount(String name){
		return StreamUtils.getCount(adminServer,name);
	}
	/**
	 * Verifies that the content of file on HDFS is the same as the data.
	 *
	 * @param data The data expected in the file.
	 * @param path The path/filename of the file on hdfs.
	 */
	public void assertValidHdfs(String data, String path) {
		validation.verifyHdfsTestContent(data, path);
	}


	/** Wait for the number of rows returned from the query  == size param or until WAIT_TIME is reached.
	 * @param query the test query to execute
	 * @param jdbcTemplate The jdbc template that has connectivity to the DB
	 * @param size The number of rows to reach before exiting the method.
	 */
	public void waitForTablePopulation(String query, JdbcTemplate jdbcTemplate, int size) {
		boolean ready = false;
		long timeout = System.currentTimeMillis() + WAIT_TIME;
		while (!ready && System.currentTimeMillis() < timeout) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e.getMessage(), e);
			}
			try {
				List<String> results = jdbcTemplate.queryForList(query, String.class);
				if (results.size() == size) {
					ready = true;
				}
			}
			catch (DataAccessException dae) {
				ready = false;
			}
		}
	}


	/**
	 * Asserts that the data stored by the file sink is what was expected.
	 *
	 * @param data The data expected in the file
	 * @param url The URL of the server that we will ssh into to get the data
	 * @param streamName the name of the stream, used to form the filename we are retrieving from the remote server
	 */
	private void assertFileContains(String data, URL url, String streamName) {
		Assert.hasText(data, "data can not be empty nor null");
		String fileName = XdEnvironment.RESULT_LOCATION + "/" + streamName
				+ ".out";
		waitForPath(pauseTime * 2000, fileName);
		validation.verifyContentContains(url, fileName, data);
	}

	/**
	 * Asserts that the data stored by a file sink, whose name is based off the stream name,
	 * is what was expected.  The assertion is case insensitive.
	 *
	 * @param data The data to validate the file content against
	 * @param url The URL of the server that we will ssh, to get the data
	 * @param streamName the name of the file we are retrieving from the remote server
	 */
	private void assertFileContainsIgnoreCase(String data, URL url, String streamName) {
		Assert.hasText(data, "data can not be empty nor null");
		String fileName = XdEnvironment.RESULT_LOCATION + "/" + streamName
				+ ".out";
		waitForPath(pauseTime * 2000, fileName);
		validation.verifyContentContainsIgnoreCase(url, fileName, data);
	}

	/**
	 * Asserts the file data to see if it matches what is expected.
	 *
	 * @param data The data to validate the file content against
	 * @param url The URL of the server that we will ssh, to get the data
	 * @param streamName the name of the file we are retrieving from the remote server
	 */
	private void assertValidFile(String data, URL url, String streamName) {
		String fileName = XdEnvironment.RESULT_LOCATION + "/" + streamName
				+ ".out";
		waitForPath(pauseTime * 2000, fileName);
		validation.verifyTestContent(url, fileName, data);
	}

	/**
	 * Waits up to the timeout for the resource to be written to filesystem.
	 *
	 * @param waitTime The number of millis to wait.
	 * @param path the path to the resource .
	 * @return false if the path was not present. True if it was present.
	 */
	private boolean waitForPath(int waitTime, String path) {
		long timeout = System.currentTimeMillis() + waitTime;
		File file = new File(path);
		boolean exists = file.exists();
		while (!exists && System.currentTimeMillis() < timeout) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e.getMessage(), e);
			}
			exists = file.exists();
		}
		return exists;
	}

	/**
	 * Asserts the log to see if the data specified is in the log.
	 *
	 * @param data The data to check if it is in the log file
	 * @param url The URL of the server we will ssh, to get the data.
	 */
	private void assertLogEntry(String data, URL url) {
		waitForXD();
		validation.verifyLogContains(url, data);
	}

	protected void waitForXD() {
		waitForXD(pauseTime * 1000);
	}

	protected void waitForXD(int millis) {
		try {
			Thread.sleep(millis);
		}
		catch (Exception ex) {
			// ignore
		}

	}

	/**
	 * Get the {@see XdEnvironment}
	 *
	 * @return the XdEnvironment
	 */
	public XdEnvironment getEnvironment() {
		return xdEnvironment;
	}

	/**
	 * Checks for file or dir on the XD instance and returns true when file is present
	 * else false.
	 * @param host The host url  where the file is be stored
	 * @param path The URI of the directory or file
	 * @return True if file is present before waitTime is expired else false.
	 */
	protected boolean fileExistsOnXDInstance(String host, String path){
		Assert.hasText(host, "host must not be null nor empty");
		Assert.hasText(path, "path must not be null nor empty");
		boolean exists = false;
		if (isOnEc2){
			exists = StreamUtils.fileExists(host,xdEnvironment.getPrivateKey(),path);
		}else{
			File file = new File(path);
			exists = file.exists();
		}
		return exists;
		
	}

}
