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

import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.impl.AbstractUserManager;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.util.Arrays;

/**
 * A dummy ftp user manager used by fixtures {@link FtpSink} and {@link FtpSource}
 *
 * @author Franck Marchand
 */
class FtpDummyUserManager extends AbstractUserManager {

	BaseUser testUser;

	public FtpDummyUserManager(File remoteServerDirectory, String username, String password) {
		super("admin", new ClearTextPasswordEncryptor());
		testUser = new BaseUser();
		testUser.setName(username);
		testUser.setPassword(password);
		testUser.setAuthorities(Arrays.asList(new Authority[]{new ConcurrentLoginPermission(3, 3),
				new WritePermission()}));
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
