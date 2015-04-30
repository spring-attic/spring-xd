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
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.impl.AbstractUserManager;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import org.springframework.xd.test.fixtures.util.AvailableSocketPorts;

/**
 * A fixture that helps testing the ftp source. Creates a local FTP server and exposes a File
 * directory where files to be picked up can be added.
 *
 * @author Eric Bottard
 */
public class FtpSource extends AbstractModuleFixture<FtpSource> implements Disposable {

	private int port = AvailableSocketPorts.nextAvailablePort();

	private File localDirectory;

	private File remoteServerDirectory;

	private FtpServer server;

	public FtpSource() {
		try {
			localDirectory = Files.createTempDirectory("ftp-source-local").toFile();
			remoteServerDirectory = Files.createTempDirectory("ftp-source-remote").toFile();
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public FtpSource ensureStarted() {
		FtpServerFactory serverFactory = new FtpServerFactory();
		serverFactory.setUserManager(new DummyUserManager());

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
		return String.format("ftp --port=%d --username=foo --password=bar --localDir=%s", port,
				localDirectory.getAbsolutePath());
	}

	@Override
	public void cleanup() {
		server.stop();
		try {
			FileUtils.deleteDirectory(localDirectory);
			FileUtils.deleteDirectory(remoteServerDirectory);
		}
		catch (IOException e) {
			throw new AssertionError("Deletion of directory should have worked", e);
		}
	}

	public File getRemoteServerDirectory() {
		return remoteServerDirectory;
	}

	private class DummyUserManager extends AbstractUserManager {

		BaseUser testUser;

		public DummyUserManager() {
			super("admin", new ClearTextPasswordEncryptor());
			testUser = new BaseUser();
			testUser.setName("foo");
			testUser.setPassword("bar");
			testUser.setAuthorities(Arrays.asList(new Authority[] { new ConcurrentLoginPermission(3, 3),
				new WritePermission() }));
			testUser.setEnabled(true);
			testUser.setHomeDirectory(remoteServerDirectory.getAbsolutePath());
		}

		@Override
		public User authenticate(Authentication arg0) throws AuthenticationFailedException {
			return testUser;
		}

		@Override
		public void delete(String arg0) throws FtpException {
		}

		@Override
		public boolean doesExist(String arg0) throws FtpException {
			return true;
		}

		@Override
		public String[] getAllUserNames() throws FtpException {
			return new String[] { "foo" };
		}

		@Override
		public User getUserByName(String arg0) throws FtpException {
			return testUser;
		}

		@Override
		public void save(User arg0) throws FtpException {
		}
	}

}
