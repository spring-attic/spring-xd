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

import org.junit.Assert;
import org.junit.Test;

import org.springframework.xd.shell.command.fixtures.FileSink;
import org.springframework.xd.shell.command.fixtures.MailSource;


/**
 * Tests for the mail related source.
 * 
 * @author Eric Bottard
 */
public class MailSourceCommandTests extends AbstractStreamIntegrationTest {

	@Test
	public void testImapPoll() throws Exception {
		MailSource mailSource = newMailSource();
		FileSink fileSink = newFileSink();

		stream().create("mailstream", "%s | %s", mailSource, fileSink);

		mailSource.sendEmail("from@foo.com", "The Subject", "My body is slim!");


		String result = fileSink.getContents();

		Assert.assertEquals("My body is slim!\r\n\n", result);
	}

}
