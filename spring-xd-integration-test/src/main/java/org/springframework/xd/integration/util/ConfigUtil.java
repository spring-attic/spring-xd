/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.integration.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.jclouds.domain.Credentials;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.http.handlers.BackoffLimitedRetryHandler;
import org.jclouds.io.payloads.FilePayload;
import org.jclouds.sshj.SshjSshClient;


/**
 * Tools to setup an XD Instance with the correct configuration files for a stream's modules.
 * 
 * @author Glenn Renfro
 */
public class ConfigUtil {

	private boolean isEc2 = false;

	private XdEnvironment environment;


	public ConfigUtil(boolean isEc2, XdEnvironment environment) {
		this.isEc2 = isEc2;
		this.environment = environment;
	}

	/**
	 * 
	 * Creates a properties file with the content you specify in the config directory of the host you specify.
	 * 
	 * @param fileName The name of the properties file.
	 * @param content The configuration data that should be put in the config.properties file.
	 * @throws IOException
	 */
	public void pushConfigToContainer(String fileName, String content) throws IOException {
		File conFile = new File("tmpConf.properties");
		conFile.deleteOnExit();
		BufferedWriter bw = new BufferedWriter(new FileWriter(conFile));
		bw.write(content);
		bw.close();
		if (!isEc2) {
			String destination = environment.getBaseDir() + "/config/" + fileName + ".properties";
			conFile.renameTo(new File(destination));
		}
		else {
			sshCopy(conFile, fileName, environment.getContainers().get(0).getHost());
		}

	}

	@SuppressWarnings("deprecation")
	private void sshCopy(File file, String fileName, String host) {
		final LoginCredentials credential = LoginCredentials
				.fromCredentials(new Credentials("ubuntu", environment.getPrivateKey()));
		final com.google.common.net.HostAndPort socket = com.google.common.net.HostAndPort
				.fromParts(host, 22);
		final SshjSshClient client = new SshjSshClient(
				new BackoffLimitedRetryHandler(), socket, credential, 5000);
		final FilePayload payload = new FilePayload(file);
		client.put(environment.getBaseDir() + "/config/" + fileName + ".properties", payload);
	}
}
