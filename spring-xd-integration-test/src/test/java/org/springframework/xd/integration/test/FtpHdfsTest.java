/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.integration.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.integration.test.util.SocketUtils;


/**
 * 
 * @author dturanski
 */
public class FtpHdfsTest extends AbstractJobTest {

	public static final String FTP_ROOT_DIR = System.getProperty("java.io.tmpdir") + File.separator + "ftproot";

	public static final String SERVER_PORT_SYSTEM_PROPERTY = "availableServerPort";

	@ClassRule
	public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

	private static FtpServer server;

	private static Logger LOGGER = LoggerFactory.getLogger(FtpHdfsTest.class);

	private static int availableServerSocket;

	@BeforeClass
	public static void setupFtpServer() throws FtpException, SocketException, IOException {


		availableServerSocket = SocketUtils.findAvailableServerSocket(4444);
		System.setProperty(SERVER_PORT_SYSTEM_PROPERTY, Integer.valueOf(availableServerSocket).toString());


		LOGGER.info("Using open server port..." + availableServerSocket);

		File ftpRoot = new File(FTP_ROOT_DIR);
		ftpRoot.mkdirs();

		FtpServerFactory serverFactory = new FtpServerFactory();
		serverFactory.setUserManager(new TestUserManager());
		ListenerFactory factory = new ListenerFactory();

		factory.setPort(availableServerSocket);

		serverFactory.addListener("default", factory.createListener());

		server = serverFactory.createServer();

		server.start();

	}

	@Test
	public void test() throws FileNotFoundException {
		//Add files
		for (int i = 0; i < 3; i++) {
			PrintWriter out = new PrintWriter(FTP_ROOT_DIR + File.separator + "file" + i + ".txt");
			out.println("hello");
			out.close();
		}
		waitForXD();
		String jobName = "ftpHdfs" + JOB_NAME;
		job(jobName, jobs.ftpHdfsJob().toDSL(), true);
		waitForXD();
		String jobParams = "{\"remoteDirectory\":\"/\",\"hdfsDirectory\":\"/ftp\"}";
		launchJob(jobName, jobParams);
		waitForXD();

		for (int i = 0; i < 3; i++) {
			String path = "/ftp/file" + i + ".txt";
			assertTrue(path + " does not exist", hadoopUtil.fileExists(path));
		}

		hadoopUtil.fileRemove("/ftp");
	}

	@AfterClass
	public static void shutDown() throws InterruptedException {
		server.stop();
		FileUtils.deleteQuietly(new File(FTP_ROOT_DIR));
	}

	static class TestUserManager extends AbstractUserManager {

		BaseUser testUser;

		public TestUserManager() {
			super("admin", new ClearTextPasswordEncryptor());
			testUser = new BaseUser();
			testUser.setName("ftpuser");
			testUser.setPassword("ftpuser");
			testUser.setAuthorities(Arrays.asList(new Authority[] { new ConcurrentLoginPermission(3, 3),
				new WritePermission() }));
			testUser.setEnabled(true);
			testUser.setHomeDirectory(FTP_ROOT_DIR);
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
			return new String[0];
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
