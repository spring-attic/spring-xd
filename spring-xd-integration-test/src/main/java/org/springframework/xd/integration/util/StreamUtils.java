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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.aws.ec2.AWSEC2Api;
import org.jclouds.aws.ec2.domain.AWSRunningInstance;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.domain.Credentials;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.ec2.domain.Reservation;
import org.jclouds.ec2.domain.RunningInstance;
import org.jclouds.http.handlers.BackoffLimitedRetryHandler;
import org.jclouds.io.payloads.ByteSourcePayload;
import org.jclouds.sshj.SshjSshClient;

import org.springframework.hateoas.PagedResources;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.xd.rest.client.impl.SpringXDTemplate;
import org.springframework.xd.rest.domain.DetailedContainerResource;
import org.springframework.xd.rest.domain.ModuleMetadataResource;
import org.springframework.xd.rest.domain.StreamDefinitionResource;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;
import com.google.common.net.HostAndPort;
import org.springframework.xd.rest.domain.metrics.CounterResource;
import org.springframework.xd.rest.domain.metrics.MetricResource;

/**
 * Utilities for creating and monitoring streams and the JMX hooks for those strings.
 *
 * @author Glenn Renfro
 */
public class StreamUtils {

	public final static String TMP_DIR = "result/";

	/**
	 * Creates the stream definition and deploys it to the cluster being tested.
	 *
	 * @param streamName The name of the stream
	 * @param streamDefinition The definition that needs to be deployed for this stream.
	 * @param adminServer The admin server that this stream will be deployed against.
	 */
	public static void stream(final String streamName, final String streamDefinition,
			final URL adminServer) {
		Assert.hasText(streamName, "The stream name must be specified.");
		Assert.hasText(streamDefinition, "a stream definition must be supplied.");
		Assert.notNull(adminServer, "The admin server must be specified.");
		createSpringXDTemplate(adminServer).streamOperations().createStream(streamName, streamDefinition, true);
	}

	/**
	 * Executes a http get for the client and returns the results as a string
	 *
	 * @param url The location to execute the get against.
	 * @return the string result of the get
	 */
	public static String httpGet(final URL url) {
		Assert.notNull(url, "The URL must be specified");
		RestTemplate template = new RestTemplate();

		try {
			String result = template.getForObject(url.toURI(), String.class);
			return result;
		}
		catch (URISyntaxException uriException) {
			throw new IllegalStateException(uriException.getMessage(), uriException);
		}
	}

	/**
	 * Removes all the streams from the cluster. Used to guarantee a clean acceptance test.
	 *
	 * @param adminServer The admin server that the command will be executed against.
	 */
	public static void destroyAllStreams(final URL adminServer) {
		Assert.notNull(adminServer, "The admin server must be specified.");
		createSpringXDTemplate(adminServer).streamOperations().destroyAll();
	}

	/**
	 * Undeploys the specified stream name
	 *
	 * @param adminServer The admin server that the command will be executed against.
	 * @param streamName The name of the stream to undeploy
	 */
	public static void undeployStream(final URL adminServer, final String streamName)
	{
		Assert.notNull(adminServer, "The admin server must be specified.");
		Assert.hasText(streamName, "The streamName must not be empty nor null");
		createSpringXDTemplate(adminServer).streamOperations().undeploy(streamName);
	}

	/**
	 * Retrieves the value for the counter
	 *
	 * @param adminServer the admin server for the cluster being tested.
	 * @param name        the name of the counter in the metric repo.
	 * @return the count associated with the name.
	 */
	public static long getCount(final URL adminServer, final String name) {
		Assert.notNull(adminServer, "The admin server must be specified.");
		Assert.hasText(name, "The name must not be empty nor null");
		CounterResource resource = createSpringXDTemplate(adminServer).
				counterOperations().retrieve(name);
		return resource.getValue();
	}

	/**
	 * Copies the specified file from a remote machine to local machine.
	 *
	 * @param privateKey the ssh private key to the remote machine
	 * @param url The remote machine's url.
	 * @param fileName The fully qualified file name of the file to be transferred.
	 * @return The location to the fully qualified file name where the remote file was copied.
	 */
	public static String transferResultsToLocal(final String privateKey, final URL url, final String fileName)
	{
		Assert.hasText(privateKey, "The Acceptance Test, can not be empty nor null.");
		Assert.notNull(url, "The remote machine's URL must be specified.");
		Assert.hasText(fileName, "The remote file name must be specified.");

		File file = new File(fileName);
		FileOutputStream fileOutputStream = null;
		InputStream inputStream = null;

		try {
			File tmpFile = createTmpDir();
			String fileLocation = tmpFile.getAbsolutePath() + file.getName();
			fileOutputStream = new FileOutputStream(fileLocation);

			final SshjSshClient client = getSSHClient(url, privateKey);
			inputStream = client.get(fileName).openStream();

			FileCopyUtils.copy(inputStream, fileOutputStream);
			return fileLocation;
		}
		catch (IOException ioException) {
			throw new IllegalStateException(ioException.getMessage(), ioException);
		}
	}

	/**
	 * Creates a file on a remote EC2 machine with the payload as its contents.
	 *
	 * @param privateKey The ssh private key for the remote container
	 * @param host The remote machine's ip.
	 * @param dir The directory to write the file
	 * @param fileName The fully qualified file name of the file to be created.
	 * @param payload the data to write to the file
	 * @param retryTime the time in millis to retry to push data file to remote system.
	 */
	public static boolean createDataFileOnRemote(String privateKey, String host, String dir, String fileName,
			String payload, int retryTime)
	{
		Assert.hasText(privateKey, "privateKey must not be empty nor null.");
		Assert.hasText(host, "The remote machine's URL must be specified.");
		Assert.notNull(dir, "dir should not be null");
		Assert.hasText(fileName, "The remote file name must be specified.");
		boolean isFileCopied = false;
		long timeout = System.currentTimeMillis() + retryTime;
		while (!isFileCopied && System.currentTimeMillis() < timeout) {
			SshjSshClient client = getSSHClient(host, privateKey);
			client.exec("mkdir -p " + dir);
			ExecResponse response = client.exec("ls -al " + dir );
			if (response.getExitStatus() > 0) {
				continue; //directory was not created
			}
			client.put(dir + "/" + fileName, payload);
			response = client.exec("ls -al " + dir + "/" + fileName);
			if (response.getExitStatus() > 0) {
				continue; //file was not created
			}
			response = client.exec("cat " + dir + "/" + fileName);
			if (response.getExitStatus() > 0 || !payload.equals(response.getOutput())) {
				continue;//data stored on machine is different than was expected.
			}
			isFileCopied = true;
		}
		return isFileCopied;
	}

	/**
	 * Copy a file from test machine to remote machine.
	 *
	 * @param privateKey The ssh private key for the remote container
	 * @param host The remote machine's ip.
	 * @param uri to the location where the file will be copied to remote machine.
	 * @param file The file to migrate to remote machine. 
	 */
	public static void copyFileToRemote(String privateKey, String host, URI uri,
			File file, long waitTime)
	{
		Assert.hasText(privateKey, "privateKey must not be empty nor null.");
		Assert.hasText(host, "The remote machine's URL must be specified.");
		Assert.notNull(uri, "uri should not be null");
		Assert.notNull(file, "file must not be null");
		boolean isFileCopied = false;
		long timeout = System.currentTimeMillis() + waitTime;
		while (!isFileCopied && System.currentTimeMillis() < timeout) {
			final SshjSshClient client = getSSHClient(host, privateKey);
			Assert.isTrue(file.exists(), "File to be copied to remote machine does not exist");
			ByteSource byteSource = com.google.common.io.Files.asByteSource(file);
			ByteSourcePayload payload = new ByteSourcePayload(byteSource);
			long byteSourceSize = -1;
			try {
				byteSourceSize = byteSource.size();
				payload.getContentMetadata().setContentLength(byteSourceSize);
			}
			catch (IOException ioe) {
				throw new IllegalStateException("Unable to retrieve size for file to be copied to remote machine.", ioe);
			}
			client.put(uri.getPath(), payload);
			if (client.exec("ls -al " + uri.getPath()).getExitStatus() == 0) {
				ExecResponse statResponse = client.exec("stat --format=%s " + uri.getPath());
				long copySize = Long.valueOf(statResponse.getOutput().trim());
				if (copySize == byteSourceSize) {
					isFileCopied = true;
				}
			}
		}

	}

	/**
	 * Creates a directory on a remote machine.  It a failure occurs it will retry until waitTime is exhausted.
	 * @param path the directory that will be created
	 * @param host the IP where the directory will be created
	 * @param privateKey Private key to be used for signing onto remote machine.
	 * @param waitTime The max time to try creating the directory
	 * @return true if the directory was successfully created else false.  
	 */
	public static boolean createRemoteDirectory(String path, String host, String privateKey, int waitTime) {
		boolean isDirectoryCreated = false;
		Assert.hasText(path, "path must not be empty nor null");
		Assert.hasText(host, "host must not be empty nor null");
		Assert.hasText(privateKey, "privateKey must not be empty nor null");
		long timeout = System.currentTimeMillis() + waitTime;
		while (!isDirectoryCreated && System.currentTimeMillis() < timeout) {
			SshjSshClient client = getSSHClient(host, privateKey);
			client.exec("mkdir " + path);
			ExecResponse response = client.exec("ls -al " + path);
			if (response.getExitStatus() > 0) {
				continue; //directory was not created
			}
			isDirectoryCreated = true;
		}
		return isDirectoryCreated;
	}

	/**
	 * Appends the payload to an existing file on a remote EC2 Instance.
	 *
	 * @param privateKey The ssh private key for the remote container
	 * @param host The remote machine's ip.
	 * @param dir The directory to write the file
	 * @param fileName The fully qualified file name of the file to be created.
	 * @param payload the data to append to the file
	 */
	public static void appendToRemoteFile(String privateKey, String host, String dir, String fileName,
			String payload)
	{
		Assert.hasText(privateKey, "privateKey must not be empty nor null.");
		Assert.hasText(host, "The remote machine's URL must be specified.");
		Assert.notNull(dir, "dir should not be null");
		Assert.hasText(fileName, "The remote file name must be specified.");

		final SshjSshClient client = getSSHClient(host, privateKey);
		client.exec("echo '" + payload + "' >> " + dir + "/" + fileName);
	}

	/**
	 * Returns a list of active instances from the specified ec2 region.
	 * @param awsAccessKey the unique id of the ec2 user.
	 * @param awsSecretKey the password of ec2 user.
	 * @param awsRegion The aws region to inspect for acceptance test instances.
	 * @return a list of active instances in the account and region specified.
	 */
	public static List<RunningInstance> getEC2RunningInstances(String awsAccessKey, String awsSecretKey,
			String awsRegion) {
		Assert.hasText(awsAccessKey, "awsAccessKey must not be empty nor null");
		Assert.hasText(awsSecretKey, "awsSecretKey must not be empty nor null");
		Assert.hasText(awsRegion, "awsRegion must not be empty nor null");

		AWSEC2Api client = ContextBuilder.newBuilder("aws-ec2")
				.credentials(awsAccessKey, awsSecretKey)
				.buildApi(AWSEC2Api.class);
		Set<? extends Reservation<? extends AWSRunningInstance>> reservations = client
				.getInstanceApi().get().describeInstancesInRegion(awsRegion);
		int instanceCount = reservations.size();
		ArrayList<RunningInstance> result = new ArrayList<RunningInstance>();
		for (int awsRunningInstanceCount = 0; awsRunningInstanceCount < instanceCount; awsRunningInstanceCount++) {
			Reservation<? extends AWSRunningInstance> instances = Iterables
					.get(reservations, awsRunningInstanceCount);
			int groupCount = instances.size();
			for (int runningInstanceCount = 0; runningInstanceCount < groupCount; runningInstanceCount++) {
				result.add(Iterables.get(instances, runningInstanceCount));
			}
		}
		return result;
	}

	/**
	 * Substitutes the port associated with the URL with another port.
	 *
	 * @param url The URL that needs a port replaced.
	 * @param port The new port number
	 * @return A new URL with the host from the URL passed in and the new port.
	 */
	public static URL replacePort(final URL url, final int port) {
		Assert.notNull(url, "the url must not be null");
		try {
			return new URL("http://" + url.getHost() + ":" + port);
		}
		catch (MalformedURLException malformedUrlException) {
			throw new IllegalStateException(malformedUrlException.getMessage(), malformedUrlException);
		}
	}

	/**
	 * Creates a map of container Id's and the associated host.
	 * @param adminServer The admin server to be queried.
	 * @return Map where the key is the container id and the value is the host ip.
	 */
	public static Map<String, String> getAvailableContainers(URL adminServer) {
		Assert.notNull(adminServer, "adminServer must not be null");
		HashMap<String, String> results = new HashMap<String, String>();
		Iterator<DetailedContainerResource> iter = createSpringXDTemplate(adminServer).runtimeOperations().listContainers().iterator();
		while (iter.hasNext()) {
			DetailedContainerResource container = iter.next();
			results.put(container.getAttribute("id"), container.getAttribute("host"));
		}
		return results;
	}

	/**
	 * Return a list of container id's where the module is deployed
	 * @param adminServer The admin server that will be queried.
	 * @return A list of containers where the module is deployed.
	 */
	public static PagedResources<ModuleMetadataResource> getRuntimeModules(URL adminServer) {
		Assert.notNull(adminServer, "adminServer must not be null");
		return createSpringXDTemplate(adminServer).runtimeOperations().listDeployedModules();
	}

	/**
	 * Waits up to the wait time for a stream to be deployed.
	 *
	 * @param streamName The name of the stream to be evaluated.
	 * @param adminServer The admin server URL that will be queried.
	 * @param waitTime the amount of time in millis to wait.
	 * @return true if the stream is deployed else false.
	 */
	public static boolean waitForStreamDeployment(String streamName, URL adminServer, int waitTime) {
		boolean result = isStreamDeployed(streamName, adminServer);
		long timeout = System.currentTimeMillis() + waitTime;
		while (!result && System.currentTimeMillis() < timeout) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e.getMessage(), e);
			}
			result = isStreamDeployed(streamName, adminServer);
		}

		return result;
	}

	/**
	 * Checks to see if the specified stream is deployed on the XD cluster.
	 *
	 * @param streamName The name of the stream to be evaluated.
	 * @param adminServer The admin server URL that will be queried.
	 * @return true if the stream is deployed else false
	 */
	public static boolean isStreamDeployed(String streamName, URL adminServer) {
		Assert.hasText(streamName, "The stream name must be specified.");
		Assert.notNull(adminServer, "The admin server must be specified.");
		boolean result = false;
		SpringXDTemplate xdTemplate = createSpringXDTemplate(adminServer);
		PagedResources<StreamDefinitionResource> resources = xdTemplate.streamOperations().list();
		Iterator<StreamDefinitionResource> resourceIter = resources.iterator();
		while (resourceIter.hasNext()) {
			StreamDefinitionResource resource = resourceIter.next();
			if (streamName.equals(resource.getName())) {
				if ("deployed".equals(resource.getStatus())) {
					result = true;
					break;
				}
				else {
					result = false;
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Waits up to the wait time for a metric to be created.
	 *
	 * @param name        The name of the metric to be evaluated.
	 * @param adminServer The admin server URL that will be queried.
	 * @param waitTime    the amount of time in millis to wait.
	 * @return true if the metric is created else false.
	 */
	public static boolean waitForMetric(String name, URL adminServer, int waitTime) {
		Assert.hasText(name, "name must not be empty nor null");
		Assert.notNull(adminServer, "The admin server must be specified.");

		SpringXDTemplate xdTemplate = createSpringXDTemplate(adminServer);
		boolean result = isMetricPresent(xdTemplate, name);
		long timeout = System.currentTimeMillis() + waitTime;
		while (!result && System.currentTimeMillis() < timeout) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e.getMessage(), e);
			}
			result = isMetricPresent(xdTemplate, name);
		}
		return result;
	}

	/**
	 * Retrieves the container pids on the local machine.  
	 * @param jpsCommand  jps command that will reveal the pids.
	 * @return An Integer array that contains the pids.
	 */
	public static Integer[] getLocalContainerPids(String jpsCommand) {
		Assert.hasText(jpsCommand, "jpsCommand can not be empty nor null");
		Integer[] result = null;
		try {
			Process p = Runtime.getRuntime().exec(jpsCommand);
			p.waitFor();
			String pidInfo = org.springframework.util.StreamUtils.copyToString(p.getInputStream(),
					Charset.forName("UTF-8"));
			result = extractPidsFromJPS(pidInfo);
		}
		catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		catch (InterruptedException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		return result;
	}

	/**
	 * Retrieves the container pids for a remote machine.  	 
	 * @param url The URL where the containers are deployed.
	 * @param privateKey ssh private key credential
	 * @param jpsCommand The command to retrieve java processes on container machine.
	 * @return An Integer array that contains the pids.
	 */
	public static Integer[] getContainerPidsFromURL(URL url, String privateKey, String jpsCommand) {
		Assert.notNull(url, "url can not be null");
		Assert.hasText(privateKey, "privateKey can not be empty nor null");
		SshjSshClient client = getSSHClient(url, privateKey);
		ExecResponse response = client.exec(jpsCommand);
		return extractPidsFromJPS(response.getOutput());
	}

	/**
	 * Verifies the specified path exists on remote machine.
	 * @param host The host where the file exists.
	 * @param privateKey ssh private key credential
	 * @param path path to file or directory
	 * @return true if file exists else false.
	 */
	public static boolean fileExists(String host, String privateKey, String path){
		Assert.hasText(host, "host must not be null nor empty");
		Assert.hasText(privateKey, "privateKey must not be null nor empty");
		Assert.hasText(path, "path must not be null nor empty");
		boolean result = true;
		final SshjSshClient client = getSSHClient(host, privateKey);
		ExecResponse response = client.exec("ls -al " + path);
		if (response.getExitStatus() > 0){
			result = false;
		}
		return result;
	}
	
	private static Integer[] extractPidsFromJPS(String jpsResult) {
		String[] pidList = StringUtils.tokenizeToStringArray(jpsResult, "\n");
		ArrayList<Integer> pids = new ArrayList<Integer>();
		for (String pidData : pidList) {
			if (pidData.contains("ContainerServerApplication")) {
				pids.add(Integer.valueOf(StringUtils.tokenizeToStringArray(pidData, " ")[0]));
			}
		}
		return pids.toArray(new Integer[pids.size()]);
	}


	private static File createTmpDir() throws IOException {
		File tmpFile = new File(System.getProperty("user.dir") + "/" + TMP_DIR);
		if (!tmpFile.exists()) {
			tmpFile.createNewFile();
		}
		return tmpFile;
	}


	private static SshjSshClient getSSHClient(URL url, String privateKey) {
		return getSSHClient(url.getHost(), privateKey);
	}

	private static SshjSshClient getSSHClient(String host, String privateKey) {
		final LoginCredentials credential = LoginCredentials
				.fromCredentials(new Credentials("ubuntu", privateKey));
		final HostAndPort socket = HostAndPort.fromParts(host, 22);
		final SshjSshClient client = new SshjSshClient(
				new BackoffLimitedRetryHandler(), socket, credential, 5000);
		return client;
	}

	/**
	 * Create an new instance of the SpringXDTemplate given the Admin Server URL
	 *
	 * @param adminServer URL of the Admin Server
	 * @return A new instance of SpringXDTemplate
	 */
	private static SpringXDTemplate createSpringXDTemplate(URL adminServer) {
		try {
			return new SpringXDTemplate(adminServer.toURI());
		}
		catch (URISyntaxException uriException) {
			throw new IllegalStateException(uriException.getMessage(), uriException);
		}
	}

	private static boolean isMetricPresent(SpringXDTemplate xdTemplate, String name) {
		boolean result = false;
		PagedResources<MetricResource> resources = xdTemplate.counterOperations().list();
		Iterator<MetricResource> resourceIter = resources.iterator();
		while (resourceIter.hasNext()) {
			MetricResource resource = resourceIter.next();
			if (name.equals(resource.getName())) {
				result = true;
				break;
			}
		}
		return result;
	}

}
