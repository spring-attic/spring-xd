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

package org.springframework.xd.shell.command.fixtures;

import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.springframework.integration.test.util.SocketUtils;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.Assert;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;


/**
 * Represents a mail source. Will create a disposable {@link GreenMail} test server.
 * 
 * @author Eric Bottard
 */
public class MailSource extends AbstractModuleFixture implements Disposable {

	private GreenMail greenMail;

	private String protocol = "imap";

	private Integer port = SocketUtils.findAvailableServerSocket(8000);

	private String folder = "INBOX";

	private ServerSetup serverSetup;

	private static final String USER = "johndoe";

	private static final String PASSWORD = "secret";

	private int smtpPort;

	@Override
	public void cleanup() {
		if (greenMail != null) {
			greenMail.stop();
		}
	}

	public MailSource protocol(String protocol) {
		ensureNotStarted();
		this.protocol = protocol;
		return this;
	}

	public MailSource port(int port) {
		ensureNotStarted();
		this.port = port;
		return this;
	}

	public void sendEmail(String from, String subject, String msg) {
		ensureStarted();
		// GreenMailUtil.sendTextEmail(USER + "@localhost.com", from, subject, msg, serverSetup);
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

		Properties props = new Properties();
		props.put("mail.debug", "true");

		mailSender.setJavaMailProperties(props);

		mailSender.setHost("localhost");
		mailSender.setPort(smtpPort);
		mailSender.setProtocol("smtp");


		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);
		try {
			helper.setTo(USER + "@localhost");
			helper.setText(msg);
			mailSender.send(message);
		}
		catch (MessagingException e) {
			throw new RuntimeException(e);
		}

	}

	public static void main(String[] args) throws Exception {
		Properties props = new Properties();
		props.put("mail.debug", "true");
		Session session = Session.getDefaultInstance(props);
		Transport transport = session.getTransport("smtp");

		ServerSetup serverSetup = new ServerSetup(8000, "localhost", "smtp");
		GreenMail greenMail = new GreenMail(serverSetup);

		System.out.println(transport);


	}

	private void ensureNotStarted() {
		Assert.state(greenMail == null, "Can't configure once started");
	}

	public MailSource ensureStarted() {
		if (greenMail == null) {
			serverSetup = new ServerSetup(port, "localhost", protocol);
			smtpPort = 8001;
			ServerSetup smtpServerSetup = new ServerSetup(smtpPort, "localhost", "smtp");

			greenMail = new GreenMail(new ServerSetup[] { serverSetup, smtpServerSetup });
			greenMail.setUser(USER + "@localhost", USER, PASSWORD);
			greenMail.start();
		}
		return this;
	}

	@Override
	protected String toDSL() {
		return String.format("mail --port=%d --protocol=%s --folder=%s --username=%s --password=%s --fixedDelay=1",
				port, protocol,
				folder, USER, PASSWORD);
	}
}
