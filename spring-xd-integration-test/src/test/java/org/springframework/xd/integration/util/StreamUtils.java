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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import org.jclouds.domain.Credentials;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.http.handlers.BackoffLimitedRetryHandler;
import org.jclouds.io.Payload;
import org.jclouds.sshj.SshjSshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HostAndPort;

/**
 * Utilities for creating and monitoring streams and the JMX hooks for those
 * strings.
 * 
 * @author Glenn Renfro
 */
public class StreamUtils {

	private final static String STREAM_SUFFIX = "/streams/";

	private static final Logger LOGGER = LoggerFactory
			.getLogger(XdEc2Validation.class);

	public static void stream(String streamName, String streamDefinition,
			URL adminServer) throws IOException {
		final URL url = new URL(adminServer + STREAM_SUFFIX);
		final String urlParameters = "name=" + streamName + "&definition="
				+ streamDefinition;
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setInstanceFollowRedirects(false);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded");
		connection.setRequestProperty("charset", "utf-8");
		connection.setRequestProperty("Content-Length",
				"" + Integer.toString(urlParameters.getBytes().length));
		connection.setUseCaches(false);

		DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				connection.getInputStream()));
		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			LOGGER.debug(inputLine);
		}
		connection.disconnect();
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
			URL adminServer) throws MalformedURLException, IOException {
		Iterator<String> streamNameIter = streamNames.iterator();
		while (streamNameIter.hasNext()) {
			URL url = new URL(adminServer + STREAM_SUFFIX
					+ streamNameIter.next());
			HttpURLConnection httpCon = (HttpURLConnection) url
					.openConnection();
			httpCon.setDoOutput(true);
			httpCon.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			httpCon.setRequestMethod("DELETE");
			httpCon.connect();
			LOGGER.debug("Delete reported a responseCode of "
					+ httpCon.getResponseCode());
			httpCon.disconnect();
		}
	}

	public static  void transferResultsToLocal(final XdEnvironment hosts, final URL url, final String fileName) throws IOException{
		final LoginCredentials credential = LoginCredentials
				.fromCredentials(new Credentials("ubuntu", hosts.getPrivateKey()));
		final HostAndPort socket = HostAndPort.fromParts(url.getHost(), 22);
		final SshjSshClient client = new SshjSshClient(
				new BackoffLimitedRetryHandler(), socket, credential, 5000);
		Payload payload = client.get(fileName);
		InputStream iStream = payload.openStream();
		BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
		boolean isRead = true;
		while (isRead) {
			byte[] b = new byte[10];
			int val = iStream.read(b);
			String buffer = new String(b,0,val);
			writer.write(buffer);
			if (val<10){
				isRead=false;
			}
		}
		writer.close();
	}
	public enum SendTypes {
		HTTP, MQTT, JMS, TCP
	}

	public static URL replacePort(URL url, int port)
			throws MalformedURLException {
		return new URL("http://" + url.getHost() + ":" + port);
	}
}
