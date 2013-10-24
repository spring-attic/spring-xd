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

package org.springframework.data.hadoop.store;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import org.springframework.data.hadoop.store.codec.Codecs;
import org.springframework.data.hadoop.store.input.TextDataReader;
import org.springframework.data.hadoop.store.output.DelimitedTextDataWriter;
import org.springframework.data.hadoop.store.output.TextDataWriter;
import org.springframework.data.hadoop.store.text.DelimitedTextStorage;


/**
 * Tests for {@code DelimitedTextStorage}.
 * 
 * @author Janne Valkealahti
 * 
 */
public class DelimitedTextStorageTests extends AbstractDataTests {

	@Test
	public void testWriteReadOneLine() throws IOException {
		DelimitedTextStorage storage = new DelimitedTextStorage(testConfig, testDefaultPath, null);
		String[] dataArray = new String[] { DATA10 };

		TextDataWriter writer = new TextDataWriter(storage, testConfig, testDefaultPath);
		TestUtils.writeDataAndClose(writer, dataArray);

		TextDataReader reader = new TextDataReader(storage, testConfig, testDefaultPath);
		TestUtils.readDataAndAssert(reader, dataArray);
	}

	@Test
	public void testWriteReadManyLines() throws IOException {
		DelimitedTextStorage storage = new DelimitedTextStorage(testConfig, testDefaultPath, null);

		TextDataWriter writer = new TextDataWriter(storage, testConfig, testDefaultPath);
		TestUtils.writeDataAndClose(writer, DATA09ARRAY);

		TextDataReader reader = new TextDataReader(storage, testConfig, testDefaultPath);
		TestUtils.readDataAndAssert(reader, DATA09ARRAY);
	}

	@Test
	public void testWriteReadManyLinesWithGzip() throws IOException {
		DelimitedTextStorage storage = new DelimitedTextStorage(testConfig, testDefaultPath,
				Codecs.GZIP.getCodecInfo());

		TextDataWriter writer = new TextDataWriter(storage, testConfig, testDefaultPath);
		TestUtils.writeDataAndClose(writer, DATA09ARRAY);

		TextDataReader reader = new TextDataReader(storage, testConfig, testDefaultPath);
		TestUtils.readDataAndAssert(reader, DATA09ARRAY);
	}

	@Test
	public void testWriteReadManyLinesWithBzip2() throws IOException {
		DelimitedTextStorage storage = new DelimitedTextStorage(testConfig, testDefaultPath,
				Codecs.BZIP2.getCodecInfo());

		TextDataWriter writer = new TextDataWriter(storage, testConfig, testDefaultPath);
		TestUtils.writeDataAndClose(writer, DATA09ARRAY);

		TextDataReader reader = new TextDataReader(storage, testConfig, testDefaultPath);
		TestUtils.readDataAndAssert(reader, DATA09ARRAY);
	}

	@Test
	public void testCsvWriteReadManyLines() throws IOException {
		DelimitedTextStorage storage = new DelimitedTextStorage(testConfig, testDefaultPath, null);

		DelimitedTextDataWriter writer = new DelimitedTextDataWriter(storage, testConfig, testDefaultPath,
				DelimitedTextDataWriter.CSV);
		TestUtils.writeDataAndClose(writer, DATA09ARRAY);

		TextDataReader reader = new TextDataReader(storage, testConfig, testDefaultPath);
		List<String> data = TestUtils.readData(reader);
		assertThat(data.size(), is(1));
	}

}
