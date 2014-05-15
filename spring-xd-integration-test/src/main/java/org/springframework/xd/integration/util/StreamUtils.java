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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;

import org.jclouds.domain.Credentials;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.http.handlers.BackoffLimitedRetryHandler;
import org.jclouds.io.Payload;
import org.jclouds.sshj.SshjSshClient;

import org.springframework.hateoas.PagedResources;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.xd.rest.client.domain.StreamDefinitionResource;
import org.springframework.xd.rest.client.impl.SpringXDTemplate;

import com.google.common.net.HostAndPort;

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
	 * Copies the specified file from a remote machine to local machine.
	 *
	 * @param xdEnvironment The environment configuration for this test
	 * @param url The remote machine's url.
	 * @param fileName The fully qualified file name of the file to be transferred.
	 * @return The location to the fully qualified file name where the remote file was copied.
	 */
	public static String transferResultsToLocal(final XdEnvironment xdEnvironment, final URL url, final String fileName)
	{
		Assert.notNull(xdEnvironment, "The Acceptance Test, require a valid xdEnvironment.");
		Assert.notNull(url, "The remote machine's URL must be specified.");
		Assert.hasText(fileName, "The remote file name must be specified.");

		File file = new File(fileName);
		FileOutputStream writer = null;
		InputStream iStream = null;

		try {
			File tmpFile = createTmpDir();
			String fileLocation = tmpFile.getAbsolutePath() + file.getName();

			final LoginCredentials credential = LoginCredentials
					.fromCredentials(new Credentials("ubuntu", xdEnvironment.getPrivateKey()));
			final HostAndPort socket = HostAndPort.fromParts(url.getHost(), 22);
			final SshjSshClient client = new SshjSshClient(
					new BackoffLimitedRetryHandler(), socket, credential, 5000);

			Payload payload = client.get(fileName);
			iStream = payload.openStream();
			writer = new FileOutputStream(fileLocation);
			writer.write(org.springframework.util.StreamUtils.copyToByteArray(iStream));
			return fileLocation;
		}
		catch (IOException ioException) {
			throw new IllegalStateException(ioException.getMessage(), ioException);
		}
		finally {
			if (writer != null) {
				try {
					writer.close();
				}
				catch (IOException ioException) {
					throw new IllegalStateException(ioException.getMessage(), ioException);
				}
			}
			if (iStream != null) {
				try {
					iStream.close();
				}
				catch (IOException ioException) {
					throw new IllegalStateException(ioException.getMessage(), ioException);
				}
			}
		}
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
				if (resource.isDeployed()) {
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

	private static File createTmpDir() throws IOException {
		File tmpFile = new File(System.getProperty("user.dir") + "/" + TMP_DIR);
		if (!tmpFile.exists()) {
			tmpFile.createNewFile();
		}
		return tmpFile;
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


}
