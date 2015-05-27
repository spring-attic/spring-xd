/*
 * Copyright 2015 the original author or authors.
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
 *
 *
 */

package org.springframework.xd.test.fixtures;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;

import org.springframework.xd.test.fixtures.util.AvailableSocketPorts;

/**
 * A fixture that helps testing the ftp sink. instantiates and runs a local FTP server and exposes a {@link File} directory in order to be able
 * to check if the file pushed through FTP is present on the server side and if it has the same content as the file sent.
 *
 * @author Franck Marchand
 */
public class FtpSink extends AbstractModuleFixture<FtpSink> implements Disposable {

	private int port = AvailableSocketPorts.nextAvailablePort();

	private File remoteServerDirectory;

	private FtpServer server;

	public FtpSink() {
		try {
			remoteServerDirectory = Files.createTempDirectory("ftp-sink-remote").toFile();
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public FtpSink ensureStarted() {
		FtpServerFactory serverFactory = new FtpServerFactory();
		serverFactory.setUserManager(new FtpDummyUserManager(remoteServerDirectory, "foo", "bar"));

		ListenerFactory factory = new ListenerFactory();
		factory.setPort(port);
		serverFactory.addListener("default", factory.createListener());

		server = serverFactory.createServer();
		try {
			server.start();
		}
		catch (FtpException e) {
			throw new IllegalStateException(e);
		}

		return this;
	}

	@Override
	protected String toDSL() {
		return String.format("ftp --port=%d --username=foo --password=bar", port);
	}

	@Override
	public void cleanup() {
		server.stop();
		try {
			FileUtils.deleteDirectory(remoteServerDirectory);
		}
		catch (IOException e) {
			throw new AssertionError("Deletion of directory should have worked", e);
		}
	}

	public File getRemoteServerDirectory() {
		return remoteServerDirectory;
	}
}
