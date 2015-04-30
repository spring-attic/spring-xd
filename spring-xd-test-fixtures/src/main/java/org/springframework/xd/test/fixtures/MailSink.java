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

import javax.mail.internet.MimeMessage;

import org.springframework.xd.test.fixtures.util.AvailableSocketPorts;

import com.icegreen.greenmail.util.ServerSetup;


/**
 * A fixture to help test the {@code mail} sink module. Allows retrieval of emails sent to the configured server.
 * 
 * @author Eric Bottard
 */
public class MailSink extends DisposableMailSupport<MailSink> {

	private int port = AvailableSocketPorts.nextAvailablePort();

	private String to;

	private String from;

	private String subject;

	private int imapPort = AvailableSocketPorts.nextAvailablePort();

	@Override
	protected ServerSetup setupSendServer() {
		return new ServerSetup(port, "localhost", "smtp");
	}

	@Override
	protected ServerSetup setupReceiveServer() {
		return new ServerSetup(imapPort, "localhost", "imap");
	}

	@Override
	protected String toDSL() {
		return String.format("mail --host=localhost --port=%d --from=%s --to=%s --subject=%s", port, from, to, subject);
	}

	public MailSink to(String expression) {
		this.to = expression;
		return this;
	}

	public MailSink subject(String expression) {
		this.subject = expression;
		return this;
	}

	public MailSink from(String expression) {
		this.from = expression;
		return this;
	}

	public MimeMessage waitForEmail() {
		ensureStarted();
		try {
			if (!greenMail.waitForIncomingEmail(1)) {
				throw new IllegalStateException("No email received");
			}
			return greenMail.getReceivedMessages()[0];
		}
		catch (InterruptedException e) {
			throw new IllegalStateException("Interrupted while waiting for an email");
		}
	}


}
