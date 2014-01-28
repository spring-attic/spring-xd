/*
 * Copyright 2013 the original author or authors.
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
 */

package org.springframework.xd.shell.command;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasContentsThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.util.StreamUtils;
import org.springframework.xd.shell.command.fixtures.FileSink;
import org.springframework.xd.shell.command.fixtures.HttpSource;


/**
 * Test http commands.
 * 
 * @author Gunnar Hillert
 * @since 1.0
 */
public class HttpCommandTests extends AbstractStreamIntegrationTest {

	private static final Log logger = LogFactory.getLog(HttpCommandTests.class);

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	/**
	 * This test will create a stream using a HTTP source and a File Sink. Subsequently the "http post" shell command is
	 * used to post a simple Ascii String to the admin server.
	 */
	@Test
	public void testHttpPostAsciiText() throws InterruptedException, IOException {

		final HttpSource httpSource = newHttpSource();

		final String stringToPost = "hello";
		final FileSink fileSink = newFileSink().binary(true);

		final String streamName = generateStreamName();
		final String stream = String.format("%s | %s", httpSource, fileSink);

		logger.info("Creating Stream: " + stream);
		stream().create(streamName, stream);

		logger.info("Posting String: " + stringToPost);
		httpSource.ensureReady().postData(stringToPost);

		assertThat(fileSink, eventually(hasContentsThat(equalTo(stringToPost))));

	}

	/**
	 * This test will create a stream using a HTTP source and a File Sink. Subsequently the "http post" shell command is
	 * used to post a UTF String (Japanese) to the admin server.
	 */
	@Test
	public void testHttpPostUtfText() throws InterruptedException, IOException {

		final HttpSource httpSource = newHttpSource();
		final FileSink fileSink = newFileSink().binary(true);

		/** I want to go to Japan. */
		final String stringToPostInJapanese = "\u65e5\u672c\u306b\u884c\u304d\u305f\u3044\u3002";

		final String streamName = generateStreamName();
		final String stream = String.format("%s | %s", httpSource, fileSink);

		logger.info("Creating Stream: " + stream);
		stream().create(streamName, stream);

		logger.info("Posting String: " + stringToPostInJapanese);
		httpSource.ensureReady().postData(stringToPostInJapanese);


		assertThat(fileSink, eventually(hasContentsThat(equalTo(stringToPostInJapanese))));

	}

	@Test
	public void testReadingFromFile() throws Exception {
		final File tempFileIn = testFolder.newFile("utfdatain.txt");
		final FileSink fileSink = newFileSink().binary(true);

		final HttpSource source = newHttpSource();

		/* I want to go to Japan. */
		final String stringToPostInJapanese = "\u65e5\u672c\u306b\u884c\u304d\u305f\u3044\u3002";
		// Let's source from an UTF16 file.
		Charset inCharset = Charset.forName("UTF-16");
		OutputStream os = new FileOutputStream(tempFileIn);
		StreamUtils.copy(stringToPostInJapanese, inCharset, os);
		os.close();

		final String streamName = generateStreamName();
		final String stream = String.format("%s | %s", source, fileSink);

		stream().create(streamName, stream);

		source.ensureReady().useContentType(String.format("text/plain;charset=%s", inCharset)).postFromFile(tempFileIn);

		assertThat(fileSink, eventually(hasContentsThat(equalTo(stringToPostInJapanese))));


	}
}
