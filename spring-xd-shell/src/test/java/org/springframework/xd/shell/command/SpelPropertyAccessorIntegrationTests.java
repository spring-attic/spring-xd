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

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for custom PropertyAccessors in SpEL expressions within streams.
 * 
 * @author Mark Fisher
 */
public class SpelPropertyAccessorIntegrationTests extends AbstractStreamIntegrationTest {

	private static final Log logger = LogFactory.getLog(StreamCommandTests.class);

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	private static final String SINK_FILE_SUFFIX = "txt";

	@Test
	public void testTuplePropertyAccessor() throws Exception {
		logger.info("Creating stream with temp File 'tupletest' as sink");
		final String tempFileName = "tupletest";
		final File tempFile = testFolder.newFile(tempFileName + "." + SINK_FILE_SUFFIX);
		final String tempFileDir = tempFile.getParentFile().getAbsolutePath();
		stream().create(
				"tupletest",
				"http --port=%s | json-to-tuple | transform --expression=payload.foo | file --name=%s --suffix=%s --dir=%s",
				DEFAULT_HTTP_PORT, tempFileName, SINK_FILE_SUFFIX, tempFileDir);
		Thread.sleep(1000);
		httpPostData("http://localhost:" + DEFAULT_HTTP_PORT, "{'foo':'bar'}");
		final String result = FileUtils.readFileToString(tempFile);
		assertEquals("bar\n", result);
	}

}
