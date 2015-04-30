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

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.xd.test.fixtures.util.AvailableSocketPorts;

import com.icegreen.greenmail.util.ServerSetup;

/**
 * Base class support for both "mail" and "imap".
 * 
 * @author Eric Bottard
 */

public abstract class AbstractMailSource<T extends AbstractMailSource<T>> extends
		DisposableMailSupport<AbstractMailSource<T>> {

	protected String protocol = "imap";

	protected int port = AvailableSocketPorts.nextAvailablePort();

	protected String folder = "INBOX";

	private int smtpPort = AvailableSocketPorts.nextAvailablePort();

	@SuppressWarnings("unchecked")
	public T protocol(String protocol) {
		ensureNotStarted();
		this.protocol = protocol;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T port(int port) {
		ensureNotStarted();
		this.port = port;
		return (T) this;

	}

	public void sendEmail(String from, String subject, String msg) {
		ensureStarted();
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

		mailSender.setHost("localhost");
		mailSender.setPort(smtpPort);
		mailSender.setProtocol("smtp");


		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);
		try {
			helper.setTo(ADMIN_USER + "@localhost");
			helper.setFrom(from);
			helper.setSubject(subject);
			helper.setText(msg);
			mailSender.send(message);
		}
		catch (MessagingException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	protected ServerSetup setupReceiveServer() {
		return new ServerSetup(port, "localhost", protocol);
	}

	@Override
	protected ServerSetup setupSendServer() {
		return new ServerSetup(smtpPort, "localhost", "smtp");
	}
}
