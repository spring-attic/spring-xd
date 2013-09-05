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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import org.springframework.xd.shell.command.fixtures.FileSink;
import org.springframework.xd.shell.command.fixtures.HttpSource;

/**
 * Tests for custom PropertyAccessors in SpEL expressions within streams.
 * 
 * @author Mark Fisher
 */
public class SpelPropertyAccessorIntegrationTests extends AbstractStreamIntegrationTest {

	private static final Log logger = LogFactory.getLog(StreamCommandTests.class);

	/**
	 * This test focuses on tuple access. Note that it explicitly creates a tuple out of Json, which is no longer
	 * required for end-user usecases (see {@link #testJsonPropertyAccessor()}).
	 */
	@Test
	public void testTuplePropertyAccessor() throws Exception {
		logger.info("Creating stream with temp File 'tupletest' as sink");
		FileSink sink = newFileSink().binary(true);
		HttpSource source = newHttpSource();

		stream().create(
				"tupletest",
				"%s | json-to-tuple | transform --expression=payload.foo | %s",
				source, sink);

		source.ensureReady().postData("{\"foo\":\"bar\"}");

		final String result = sink.getContents();
		assertEquals("bar", result.trim());
	}

	/**
	 * This tests that we have Json property access out of the box.
	 */
	@Test
	public void testJsonPropertyAccessor() throws Exception {
		FileSink sink = newFileSink().binary(true);
		HttpSource source = newHttpSource();

		stream().create(
				"jsontest",
				"%s | transform --expression=payload.foo | %s",
				source, sink);

		source.ensureReady().postData("{\"foo\":\"bar\"}");

		final String result = sink.getContents();
		assertEquals("bar", result);

	}

}
