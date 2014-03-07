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
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.jclouds.domain.Credentials;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.http.handlers.BackoffLimitedRetryHandler;
import org.jclouds.io.Payload;
import org.jclouds.sshj.SshjSshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.xd.rest.client.impl.SpringXDTemplate;

import com.google.common.net.HostAndPort;

/**
 * Utilities for creating and monitoring streams and the JMX hooks for those strings.
 * 
 * @author Glenn Renfro
 */
public class StreamUtils {

	public final static String TMP_DIR = "result/";

	private final static String STREAM_SUFFIX = "/streams/";

	private static final Logger LOGGER = LoggerFactory
			.getLogger(XdEc2Validation.class);

	public static void stream(String streamName, String streamDefinition,
			URL adminServer) throws IOException, URISyntaxException {
		SpringXDTemplate xdTemplate = new SpringXDTemplate(adminServer.toURI());
		System.out.println(streamName);
		System.out.println(streamDefinition);
		xdTemplate.streamOperations().createStream(streamName, streamDefinition, true);
	}

	public static void send(SendTypes method, String message, URL url)
			throws IOException {

		if (method.equals(SendTypes.HTTP)) {
			httpPost(message, url);
		}

	}

	public static void httpPost(String message, URL url) throws IOException,
			MalformedURLException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		// Let's send this as a post
		conn.setRequestMethod("POST");

		// Send post request
		conn.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
		wr.writeBytes(message);
		wr.flush();
		wr.close();
		LOGGER.debug("httpPost result code was " + conn.getResponseCode());
		conn.disconnect();

	}

	public static String httpGet(URL url) throws IOException {
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

	public static void destroyAllStreams(List<String> streamNames,
			URL adminServer) throws URISyntaxException, IOException {
		SpringXDTemplate xdTemplate = new SpringXDTemplate(adminServer.toURI());
		xdTemplate.streamOperations().destroyAll();
	}

	public static String transferResultsToLocal(final XdEnvironment hosts, final URL url, final String fileName)
			throws IOException {
		File file = new File(fileName);
		File logFile = createTmpDir();
		String fileLocation = logFile.getAbsolutePath() + file.getName();

		final LoginCredentials credential = LoginCredentials
				.fromCredentials(new Credentials("ubuntu", hosts.getPrivateKey()));
		final HostAndPort socket = HostAndPort.fromParts(url.getHost(), 22);
		final SshjSshClient client = new SshjSshClient(
				new BackoffLimitedRetryHandler(), socket, credential, 5000);
		Payload payload = client.get(fileName);
		InputStream iStream = payload.openStream();
		BufferedWriter writer = new BufferedWriter(new FileWriter(fileLocation));
		boolean isRead = true;
		while (isRead) {
			byte[] b = new byte[10];
			int val = iStream.read(b);
			String buffer = new String(b, 0, val);
			writer.write(buffer);
			if (val < 10) {
				isRead = false;
			}
		}
		writer.close();
		return fileLocation;
	}

	public static String transferLogToTmp(final XdEnvironment hosts, final URL url, final String fileName)
			throws IOException {
		final LoginCredentials credential = LoginCredentials
				.fromCredentials(new Credentials("ubuntu", hosts.getPrivateKey()));
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

	private static File createTmpDir() throws IOException {
		File tmpFile = new File(System.getProperty("user.dir") + "/" + TMP_DIR);
		if (!tmpFile.exists()) {
			tmpFile.createNewFile();
		}
		return tmpFile;
	}

	public enum SendTypes {
		HTTP, MQTT, JMS, TCP
	}

	public static URL replacePort(URL url, int port)
			throws MalformedURLException {
		return new URL("http://" + url.getHost() + ":" + port);
	}
}
