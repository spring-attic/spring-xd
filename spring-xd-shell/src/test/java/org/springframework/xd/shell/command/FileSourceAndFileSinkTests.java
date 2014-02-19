/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasContentsThat;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.junit.Before;
import org.junit.Test;

import org.springframework.util.FileCopyUtils;
import org.springframework.xd.shell.command.fixtures.FileSink;
import org.springframework.xd.shell.command.fixtures.FileSource;

/**
 * Tests to explicitly assert file related source and sink behavior.
 * 
 * @author Eric Bottard
 * @author David Turanski
 */
public class FileSourceAndFileSinkTests extends AbstractStreamIntegrationTest {

	private static final File DEFAULT_IN = new File("/tmp/xd/input");

	private static final File DEFAULT_OUT = new File("/tmp/xd/output");

	private static final String DEFAULT_SINKFILE_SUFFIX = "out";

	private static final String TEXT_SINKFILE_SUFFIX = "txt";

	@Before
	public void before() {
		DEFAULT_OUT.mkdirs();
	}

	@Test
	public void testDefaultFileLocations() throws Exception {
		testFileSinkSource("." + DEFAULT_SINKFILE_SUFFIX, "");
	}

	@Test
	public void testBlankSuffix() throws Exception {
		testFileSinkSource("", "--suffix=' '");
	}

	@Test
	public void testCustomSuffix() throws Exception {
		testFileSinkSource("." + TEXT_SINKFILE_SUFFIX, "--suffix='" + TEXT_SINKFILE_SUFFIX + "'");
	}

	@Test
	public void testCustomFileLocations() throws Exception {
		// Are we on *nix at least?
		if (!new File("/tmp/").exists()) {
			return;
		}

		FileSource source = newFileSource();
		FileSink sink = newFileSink().binary(true);

		source.appendToFile("Hi there!");
		stream().create(generateStreamName(), "%s | %s", source, sink);
		assertThat(sink, eventually(hasContentsThat(equalTo("Hi there!"))));


	}

	private void testFileSinkSource(String sinkSuffix, String sinkParam) throws Exception {
		// Are we on *nix at least?
		if (!new File("/tmp/").exists()) {
			return;
		}

		// Both use stream name
		String streamName = generateStreamName();

		File inDir = new File(DEFAULT_IN, streamName);
		inDir.mkdirs();
		File in = new File(inDir, "one.txt");
		File out = new File(DEFAULT_OUT, streamName + sinkSuffix);

		try {
			FileCopyUtils.copy("hello", new FileWriter(in));
			stream().create(streamName, "file --outputType=text/plain | file " + sinkParam);
			Thread.sleep(1000);
			String actual = FileCopyUtils.copyToString(new FileReader(out));
			assertEquals("hello", actual.trim());
		}
		finally {
			in.delete();
			in.getParentFile().delete();
			out.delete();
		}
	}
}
