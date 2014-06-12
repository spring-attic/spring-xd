/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.test.fixtures;

import org.springframework.util.Assert;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;


/**
 * Base class for fixtures that interact with a mail server. Uses the {@link GreenMail} library and makes sure it is
 * properly shut down.
 * 
 * <p>
 * Creates two server setups, one for sending and one for receiving. Depending on the type of module being tested, one
 * will be used as the real server and the other will be used to interact with the former, or vice-versa.
 * </p>
 * 
 * @param <T> the type of this
 * 
 * @author Eric Bottard
 */
public abstract class DisposableMailSupport<T extends DisposableMailSupport<T>> extends
		AbstractModuleFixture<T> implements
		Disposable {

	protected static final String ADMIN_USER = "johndoe";

	protected static final String ADMIN_PASSWORD = "secret";

	protected GreenMail greenMail;

	private ServerSetup send;

	private ServerSetup receive;

	@Override
	public void cleanup() {
		if (greenMail != null) {
			greenMail.stop();
		}
	}

	@SuppressWarnings("unchecked")
	protected T ensureNotStarted() {
		Assert.state(greenMail == null, "Can't configure once started");
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T ensureStarted() {
		if (greenMail == null) {
			send = setupSendServer();
			receive = setupReceiveServer();

			greenMail = new GreenMail(new ServerSetup[] { send, receive });
			greenMail.setUser(ADMIN_USER + "@localhost", ADMIN_USER, ADMIN_PASSWORD);
			greenMail.start();
		}
		return (T) this;
	}

	/**
	 * Provide configuration options for the "send" part of the fixture.
	 */
	protected abstract ServerSetup setupSendServer();

	/**
	 * Provide configuration options for the "receive" part of the fixture.
	 */
	protected abstract ServerSetup setupReceiveServer();


}
