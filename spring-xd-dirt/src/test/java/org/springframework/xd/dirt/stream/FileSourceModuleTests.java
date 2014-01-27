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

package org.springframework.xd.dirt.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.integration.x.bus.DefaultMessageMediaTypeResolver;
import org.springframework.integration.x.bus.MessageMediaTypeResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;


/**
 * 
 * @author David Turanski
 */
public class FileSourceModuleTests extends StreamTestSupport {

	private static String tmpDirName = System.getProperty("java.io.tmpdir");

	private static String sourceDirName =
			tmpDirName + (tmpDirName.endsWith("/") ? "" : "/") + "filesourcetests";

	private static File sourceDir = new File(sourceDirName);

	MessageMediaTypeResolver mediaTypeResolver = new DefaultMessageMediaTypeResolver();

	@BeforeClass
	public static void createTempDir() throws IOException {
		FileUtils.forceMkdir(sourceDir);
	}

	@Before
	public void setUp() throws IOException {
		if (sourceDir.exists()) {
			FileUtils.cleanDirectory(sourceDir);
		}
	}

	@Test
	public void testFileContents() throws IOException {
		deployStream(
				"filecontents",
				"file --dir=" + sourceDirName + " --fixedDelay=0 | sink");
		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				byte[] bytes = (byte[]) message.getPayload();
				assertEquals("foo", new String(bytes));
				assertEquals(MediaType.valueOf("application/octet-stream"), mediaTypeResolver.resolveMediaType(message));
			}
		};
		StreamTestSupport.getSinkInputChannel("filecontents").subscribe(test);
		dropFile("foo.txt");
		test.waitForCompletion(1000);
		StreamTestSupport.getDeployedModule("filecontents", 0).stop();
		assertTrue(test.getMessageHandled());
	}

	@Test
	public void testFileContentsAsString() throws IOException {
		deployStream(
				"filestring",
				"file --dir=" + sourceDirName + " --fixedDelay=0 --outputType='text/plain;charset=UTF-8' | sink");
		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				assertEquals("foo", message.getPayload());
				assertEquals(MediaType.valueOf("text/plain;charset=UTF-8"), mediaTypeResolver.resolveMediaType(message));
			}
		};
		StreamTestSupport.getSinkInputChannel("filestring").subscribe(test);
		dropFile("foo2.txt");
		test.waitForCompletion(1000);
		StreamTestSupport.getDeployedModule("filestring", 0).stop();
		assertTrue(test.getMessageHandled());
	}

	@Test
	public void testFileReference() throws IOException {
		deployStream(
				"fileref",
				"file --ref=true --dir=" + sourceDirName + " --fixedDelay=0 | sink");
		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				File file = (File) message.getPayload();
				assertEquals(sourceDirName + "/" + "foo1.txt", file.getAbsolutePath());
			}
		};
		StreamTestSupport.getSinkInputChannel("fileref").subscribe(test);
		dropFile("foo1.txt");
		test.waitForCompletion(1000);
		StreamTestSupport.getDeployedModule("fileref", 0).stop();
		assertTrue(test.getMessageHandled());
	}

	private void dropFile(String fileName) throws IOException {
		PrintWriter writer = new PrintWriter(sourceDirName + "/" + fileName, "UTF-8");
		writer.write("foo");
		writer.close();
	}

	@AfterClass
	public static void deleteTempDir() throws IOException {
		if (sourceDir.exists()) {
			FileUtils.cleanDirectory(sourceDir);
			FileUtils.deleteDirectory(sourceDir);
		}
	}
}
