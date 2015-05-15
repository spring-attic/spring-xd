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

package org.springframework.xd.shell.command;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasContentsThat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Test;

import org.springframework.xd.test.fixtures.FileSink;
import org.springframework.xd.test.fixtures.FtpSource;

/**
 * Tests for the FTP source.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 */
public class FtpModulesTests extends AbstractStreamIntegrationTest {

	@Test
	public void testBasicModuleBehavior() throws IOException {
		FtpSource ftpSource = newFtpSource();
		FileSink fileSink = newFileSink();

		File file = new File(ftpSource.getRemoteServerDirectory(), "hello.txt");
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write("foobar");
		fileWriter.close();

		ftpSource.ensureStarted();

		stream().create(generateStreamName(), "%s | %s --inputType=text/plain", ftpSource, fileSink);

		assertThat(fileSink, eventually(hasContentsThat(equalTo("foobar\n"))));
	}

	@Test
	public void testModeOptionEqualsFileAsBytes() throws Exception {
		FtpSource ftpSource = newFtpSource();
		FileSink fileSink = newFileSink();

		File file = new File(ftpSource.getRemoteServerDirectory(), "hello.txt");
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write("foobar");
		fileWriter.close();

		ftpSource.ensureStarted();

		stream().create(generateStreamName(), "%s --mode=fileAsBytes | transform --expression=payload.getClass() | %s",
				ftpSource, fileSink);

		assertThat(fileSink, eventually(hasContentsThat(equalTo("byte[]\n"))));

	}

	@Test
	public void testModeOptionEqualsDefaultFileAsBytes() throws Exception {
		FtpSource ftpSource = newFtpSource();
		FileSink fileSink = newFileSink();

		File file = new File(ftpSource.getRemoteServerDirectory(), "hello.txt");
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write("foobar");
		fileWriter.close();

		ftpSource.ensureStarted();

		stream().create(generateStreamName(), "%s | transform --expression=payload.getClass() | %s",
				ftpSource, fileSink);

		assertThat(fileSink, eventually(hasContentsThat(equalTo("byte[]\n"))));

	}

	@Test
	public void testModeOptionEqualsRef() throws Exception {
		FtpSource ftpSource = newFtpSource();
		FileSink fileSink = newFileSink();

		File file = new File(ftpSource.getRemoteServerDirectory(), "hello.txt");
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write("foobar");
		fileWriter.close();

		ftpSource.ensureStarted();

		stream().create(generateStreamName(), "%s --mode=ref | transform --expression=payload.getClass() | %s",
				ftpSource, fileSink);

		assertThat(fileSink, eventually(hasContentsThat(equalTo("java.io.File\n"))));

	}

	@Test
	public void testModeOptionEqualsTextLine() throws Exception {
		FtpSource ftpSource = newFtpSource();
		FileSink fileSink = newFileSink();

		File file = new File(ftpSource.getRemoteServerDirectory(), "hello.txt");
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write("foobar");
		fileWriter.close();

		ftpSource.ensureStarted();

		stream().create(generateStreamName(), "%s --mode=textLine | transform --expression=payload.getClass() | %s",
				ftpSource, fileSink);

		assertThat(fileSink, eventually(hasContentsThat(equalTo("java.lang.String\n"))));

	}

}
