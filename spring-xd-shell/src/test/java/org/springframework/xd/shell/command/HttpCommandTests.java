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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.integration.test.util.SocketUtils;

import static org.junit.Assert.*;

/**
 * Test http commands
 * 
 * @author Gunnar Hillert
 * @since 1.0
 */
public class HttpCommandTests extends AbstractStreamIntegrationTest {

	private static final Log logger = LogFactory.getLog(HttpCommandTests.class);

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	private static final String SINK_FILE_SUFFIX = "txt";

	/**
	 * This test will create a stream using a HTTP source and a File Sink. Subsequently
	 * the "http post" shell command is used to post a simple Ascii String to the admin
	 * server.
	 */
	@Test
	public void testHttpPostAsciiText() throws InterruptedException, IOException {

		final int openPort = SocketUtils.findAvailableServerSocket(8000);
		final String tempFileName = "asciidata";
		final File tempFile = testFolder.newFile(tempFileName + "." + SINK_FILE_SUFFIX);
		final String tempFileDir = tempFile.getParentFile().getAbsolutePath();

		final String stringToPost = "hello";

		final String streamName = "postAsciiData";
		final String stream = String.format("http --port=%s | file --name=%s --suffix=%s --dir=%s", openPort,
				tempFileName, SINK_FILE_SUFFIX, tempFileDir);

		logger.info("Creating Stream: " + stream);
		stream().create(streamName, stream);

		logger.info("Posting String: " + stringToPost);
		getShell().executeCommand(
				String.format("http post --target http://localhost:%s --data \"%s\"", openPort, stringToPost));

		final String resultData = FileUtils.readFileToString(tempFile);
		assertEquals(stringToPost + "\n", resultData);
	}

	/**
	 * This test will create a stream using a HTTP source and a File Sink. Subsequently
	 * the "http post" shell command is used to post a UTF String (Japanese) to the admin
	 * server.
	 */
	@Test
	public void testHttpPostUtfText() throws InterruptedException, IOException {

		final int openPort = SocketUtils.findAvailableServerSocket(8200);
		final String tempFileName = "utfdata";
		final File tempFile = testFolder.newFile("utfdata" + "." + SINK_FILE_SUFFIX);
		final String tempFileDir = tempFile.getParentFile().getAbsolutePath();

		/** I want to go to Japan. */
		final String stringToPostInJapanese = "\u65e5\u672c\u306b\u884c\u304d\u305f\u3044\u3002";

		final String streamName = "postUtf8Data";
		final String stream = String.format("http --port=%s | file --name=%s --suffix=%s --dir=%s", openPort,
				tempFileName, SINK_FILE_SUFFIX, tempFileDir);

		logger.info("Creating Stream: " + stream);
		stream().create(streamName, stream);

		logger.info("Posting String: " + stringToPostInJapanese);
		getShell().executeCommand(
				String.format("http post --target http://localhost:%s --data \"%s\"", openPort, stringToPostInJapanese));

		final String resultData = FileUtils.readFileToString(tempFile, "UTF-8");
		assertEquals(stringToPostInJapanese + "\n", resultData);
	}

	@Test
	public void testReadingFromFile() throws Exception {
		final int openPort = SocketUtils.findAvailableServerSocket(8300);
		final String tempFileOutName = "utfdataout";
		final File tempFileOut = testFolder.newFile(tempFileOutName + "." + SINK_FILE_SUFFIX);
		final File tempFileIn = testFolder.newFile("utfdatain.txt");
		final String tempFileDir = tempFileOut.getParentFile().getAbsolutePath();

		/** I want to go to Japan. */
		final String stringToPostInJapanese = "\u65e5\u672c\u306b\u884c\u304d\u305f\u3044\u3002";
		// Let's source from an UTF16 file.
		Charset inCharset = Charset.forName("UTF-16");
		FileUtils.writeStringToFile(tempFileIn, stringToPostInJapanese, inCharset);

		final String streamName = "postUtf8Data";
		final String stream = String.format("http --port=%s | file --name=%s --suffix=%s --dir=%s", openPort,
				tempFileOutName, SINK_FILE_SUFFIX, tempFileDir);

		stream().create(streamName, stream);

		getShell().executeCommand(
				String.format(
						"http post --target http://localhost:%s --file %s --contentType \"text/plain;charset=%s\"",
						openPort, tempFileIn.getAbsolutePath(), inCharset));

		Thread.sleep(2000);
		// Using UTF-8 here, as this is the default of file sink.
		final String resultData = FileUtils.readFileToString(tempFileOut, "UTF-8");
		assertEquals(stringToPostInJapanese + "\n", resultData);

	}
}
