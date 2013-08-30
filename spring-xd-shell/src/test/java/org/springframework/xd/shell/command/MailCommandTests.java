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

package org.springframework.xd.shell.command;

import javax.mail.internet.MimeMessage;

import org.junit.Assert;
import org.junit.Test;

import org.springframework.xd.shell.command.fixtures.FileSink;
import org.springframework.xd.shell.command.fixtures.HttpSource;
import org.springframework.xd.shell.command.fixtures.MailSink;
import org.springframework.xd.shell.command.fixtures.MailSource;


/**
 * Tests for the mail related sources and sink.
 * 
 * @author Eric Bottard
 */
public class MailCommandTests extends AbstractStreamIntegrationTest {

	@Test
	public void testImapPoll() throws Exception {
		MailSource mailSource = newMailSource();
		FileSink fileSink = newFileSink();

		mailSource.ensureStarted();

		stream().create("mailstream", "%s | %s", mailSource, fileSink);

		mailSource.sendEmail("from@foo.com", "The Subject", "My body is slim!");

		String result = fileSink.getContents();

		Assert.assertEquals("My body is slim!\r\n\n", result);
	}

	@Test
	public void testMailSink() throws Exception {
		HttpSource httpSource = newHttpSource();
		MailSink mailSink = newMailSink();

		mailSink.ensureStarted()
				.to("'\"some.one@domain.com\"'")
				.subject("payload");


		stream().create("mailstream", "%s | %s", httpSource, mailSink);

		httpSource.ensureReady().postData("Woohoo!");
		MimeMessage result = mailSink.waitForEmail();

		Assert.assertEquals("Woohoo!\r\n", result.getContent());
		Assert.assertEquals("Woohoo!", result.getSubject());

	}

}
