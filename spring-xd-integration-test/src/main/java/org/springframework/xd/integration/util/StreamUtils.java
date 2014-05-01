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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.jclouds.domain.Credentials;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.http.handlers.BackoffLimitedRetryHandler;
import org.jclouds.io.Payload;
import org.jclouds.sshj.SshjSshClient;

import org.springframework.util.Assert;
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
		try {
			SpringXDTemplate xdTemplate = new SpringXDTemplate(adminServer.toURI());
			xdTemplate.streamOperations().createStream(streamName, streamDefinition, true);
		}
		catch (URISyntaxException uriException) {
			throw new IllegalStateException(uriException.getMessage());
		}
	}

	/**
	 * Executes a http get for the client and returns the results as a string
	 *
	 * @param url The location to execute the get against.
	 * @return the string result of the get
	 */
	public static String httpGet(final URL url) {
		Assert.notNull(url, "The URL must be specified");
		try {
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			InputStream response = conn.getInputStream();
			BufferedInputStream in = new BufferedInputStream(response);
			BufferedReader diReader = new BufferedReader(new InputStreamReader(in));
			String result = "";
			while (diReader.ready()) {
				result += diReader.readLine();
			}
			diReader.close();
			conn.disconnect();
			return result;
		}
		catch (IOException ioException) {
			throw new IllegalStateException(ioException.getMessage());
		}
	}

	/**
	 * Removes all the streams from the cluster. Used to guarantee a clean acceptance test.
	 *
	 * @param adminServer The admin server that the command will be executed against.
	 */
	public static void destroyAllStreams(final URL adminServer) {
		Assert.notNull(adminServer, "The admin server must be specified.");
		try {
			SpringXDTemplate xdTemplate = new SpringXDTemplate(adminServer.toURI());
			xdTemplate.streamOperations().destroyAll();
		}
		catch (URISyntaxException uriException) {
			throw new IllegalStateException(uriException.getMessage());
		}

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
		try {
			File tmpFile = createTmpDir();
			String fileLocation = tmpFile.getAbsolutePath() + file.getName();

			final LoginCredentials credential = LoginCredentials
					.fromCredentials(new Credentials("ubuntu", xdEnvironment.getPrivateKey()));
			final HostAndPort socket = HostAndPort.fromParts(url.getHost(), 22);
			final SshjSshClient client = new SshjSshClient(
					new BackoffLimitedRetryHandler(), socket, credential, 5000);
			Payload payload = client.get(fileName);
			InputStream iStream = payload.openStream();
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileLocation));
			boolean isRead = true;
			while (isRead) {
				byte[] b = new byte[10];
				int bytesRead = iStream.read(b);
				String buffer = new String(b, 0, bytesRead);
				writer.write(buffer);
				if (bytesRead < 10) {
					isRead = false;
				}
			}
			writer.close();
			return fileLocation;
		}
		catch (IOException ioException) {
			throw new IllegalStateException(ioException.getMessage());
		}
	}

	/**
	 * Copies the specified xd log file from a remote machine to local machine.
	 *
	 * @param xdEnvironment The environment configuration for this test
	 * @param url The remote machine's url.
	 * @param fileName The fully qualified file name of the file to be transferred.
	 * @return The location to the fully qualified file name where the remote file was copied.
	 */

	public static String transferLogToTmp(final XdEnvironment xdEnvironment, final URL url, final String fileName) {
		Assert.notNull(xdEnvironment, "The Acceptance Test, require a valid xdEnvironment.");
		Assert.notNull(url, "The remote machine's URL must be specified.");
		Assert.hasText(fileName, "The remote file name must be specified.");

		try {
			final LoginCredentials credential = LoginCredentials
					.fromCredentials(new Credentials("ubuntu", xdEnvironment.getPrivateKey()));
			final HostAndPort socket = HostAndPort.fromParts(url.getHost(), 22);
			final SshjSshClient client = new SshjSshClient(
					new BackoffLimitedRetryHandler(), socket, credential, 5000);
			Payload payload = client.get(fileName);
			InputStream iStream = payload.openStream();
			File logFile = createTmpDir();
			String logLocation = logFile.getAbsolutePath() + "log.out";

			BufferedWriter writer = new BufferedWriter(new FileWriter(logLocation));
			boolean isRead = true;
			while (isRead) {
				byte[] b = new byte[10];
				int val = iStream.read(b);
				String buffer = new String(b, 0, b.length);
				writer.write(buffer);
				if (val < 10) {
					isRead = false;
				}
			}
			writer.close();
			return logLocation;
		}
		catch (IOException ioException) {
			throw new IllegalStateException(ioException.getMessage());
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
			throw new IllegalStateException(malformedUrlException.getMessage());
		}
	}

	private static File createTmpDir() throws IOException {
		File tmpFile = new File(System.getProperty("user.dir") + "/" + TMP_DIR);
		if (!tmpFile.exists()) {
			tmpFile.createNewFile();
		}
		return tmpFile;
	}


}
