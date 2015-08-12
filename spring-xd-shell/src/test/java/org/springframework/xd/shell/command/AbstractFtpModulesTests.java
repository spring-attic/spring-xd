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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasTrimmedContentsThat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

import org.springframework.xd.test.fixtures.FileSink;
import org.springframework.xd.test.fixtures.FileSource;
import org.springframework.xd.test.fixtures.FtpSink;
import org.springframework.xd.test.fixtures.FtpSource;

/**
 * Tests for the FTP source.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 * @author Franck Marchand
 * @author David Turanski
 */
public class AbstractFtpModulesTests extends AbstractStreamIntegrationTest {

	public static final String HELLO_FTP_SINK = "hello ftp sink !";

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

		assertThat(fileSink, eventually(hasTrimmedContentsThat(equalTo("foobar"))));
	}

	@Test
	public void testModeOptionEqualsContents() throws Exception {
		FtpSource ftpSource = newFtpSource();
		FileSink fileSink = newFileSink();

		File file = new File(ftpSource.getRemoteServerDirectory(), "hello.txt");
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write("foobar");
		fileWriter.close();

		ftpSource.ensureStarted();

		stream().create(generateStreamName(), "%s --mode=contents | transform --expression=payload.getClass() | %s",
				ftpSource, fileSink);

		assertThat(fileSink, eventually(hasTrimmedContentsThat(equalTo("byte[]"))));

	}

	@Test
	public void testModeOptionEqualsDefaultContents() throws Exception {
		FtpSource ftpSource = newFtpSource();
		FileSink fileSink = newFileSink();

		File file = new File(ftpSource.getRemoteServerDirectory(), "hello.txt");
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write("foobar");
		fileWriter.close();

		ftpSource.ensureStarted();

		stream().create(generateStreamName(), "%s | transform --expression=payload.getClass() | %s",
				ftpSource, fileSink);

		assertThat(fileSink, eventually(hasTrimmedContentsThat(equalTo("byte[]"))));

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

		assertThat(fileSink, eventually(hasTrimmedContentsThat(equalTo("java.io.File"))));

	}

	@Test
	public void testModeOptionEqualsLines() throws Exception {
		FtpSource ftpSource = newFtpSource();
		FileSink fileSink = newFileSink();

		File file = new File(ftpSource.getRemoteServerDirectory(), "hello.txt");
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write("foobar");
		fileWriter.close();

		ftpSource.ensureStarted();

		stream().create(generateStreamName(), "%s --mode=lines | transform --expression=payload.getClass() | %s",
				ftpSource, fileSink);

		assertThat(fileSink, eventually(hasTrimmedContentsThat(equalTo("java.lang.String"))));

	}

	@Test
	public void testModeOptionEqualsLinesWithUppercase() throws Exception {
		FtpSource ftpSource = newFtpSource();
		FileSink fileSink = newFileSink();

		File file = new File(ftpSource.getRemoteServerDirectory(), "hello.txt");
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write("foobar");
		fileWriter.close();

		ftpSource.ensureStarted();

		stream().create(generateStreamName(), "%s --mode=LINes | transform --expression=payload.getClass() | %s",
				ftpSource, fileSink);

		assertThat(fileSink, eventually(hasTrimmedContentsThat(equalTo("java.lang.String"))));
	}

	@Test
	public void testBasicSinkModuleBehavior() throws IOException {
		FtpSink ftpSink = newFtpSink();
		FileSource fileSource = newFileSource();

		fileSource.appendToFile(HELLO_FTP_SINK);

		ftpSink.ensureStarted();

		stream().create(generateStreamName(), "%s | %s", fileSource, ftpSink);

		String[] files = ftpSink.getRemoteServerDirectory().list();

		assertThat("only one file should have been created !", files.length, is(1));

		List<String> lines = Files.readAllLines(
				Paths.get(ftpSink.getRemoteServerDirectory().getAbsolutePath(), files[0]), StandardCharsets.UTF_8);

		assertThat(lines, hasSize(1));
		assertThat(lines, contains(HELLO_FTP_SINK));
	}
}
