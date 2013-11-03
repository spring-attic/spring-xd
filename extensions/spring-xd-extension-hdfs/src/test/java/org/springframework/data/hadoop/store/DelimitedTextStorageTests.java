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
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import org.springframework.data.hadoop.store.codec.Codecs;
import org.springframework.data.hadoop.store.input.DelimitedTextEntityReader;
import org.springframework.data.hadoop.store.input.TextEntityReader;
import org.springframework.data.hadoop.store.naming.RollingFileNamingStrategy;
import org.springframework.data.hadoop.store.output.DelimitedTextEntityWriter;
import org.springframework.data.hadoop.store.output.TextEntityWriter;
import org.springframework.data.hadoop.store.rollover.SizeRolloverStrategy;
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

		TextEntityWriter writer = new TextEntityWriter(storage, testConfig, testDefaultPath);
		TestUtils.writeDataAndClose(writer, dataArray);

		TextEntityReader reader = new TextEntityReader(storage, testConfig, testDefaultPath);
		TestUtils.readDataAndAssert(reader, dataArray);
	}

	@Test
	public void testWriteReadManyLines() throws IOException {
		DelimitedTextStorage storage = new DelimitedTextStorage(testConfig, testDefaultPath, null);

		TextEntityWriter writer = new TextEntityWriter(storage, testConfig, testDefaultPath);
		TestUtils.writeDataAndClose(writer, DATA09ARRAY);

		TextEntityReader reader = new TextEntityReader(storage, testConfig, testDefaultPath);
		TestUtils.readDataAndAssert(reader, DATA09ARRAY);
	}

	@Test
	public void testWriteReadManyLinesWithGzip() throws IOException {
		DelimitedTextStorage storage = new DelimitedTextStorage(testConfig, testDefaultPath,
				Codecs.GZIP.getCodecInfo());

		TextEntityWriter writer = new TextEntityWriter(storage, testConfig, testDefaultPath);
		TestUtils.writeDataAndClose(writer, DATA09ARRAY);

		TextEntityReader reader = new TextEntityReader(storage, testConfig, testDefaultPath);
		TestUtils.readDataAndAssert(reader, DATA09ARRAY);
	}

	@Test
	public void testWriteReadManyLinesWithBzip2() throws IOException {
		DelimitedTextStorage storage = new DelimitedTextStorage(testConfig, testDefaultPath,
				Codecs.BZIP2.getCodecInfo());

		TextEntityWriter writer = new TextEntityWriter(storage, testConfig, testDefaultPath);
		TestUtils.writeDataAndClose(writer, DATA09ARRAY);

		TextEntityReader reader = new TextEntityReader(storage, testConfig, testDefaultPath);
		TestUtils.readDataAndAssert(reader, DATA09ARRAY);
	}

	@Test
	public void testCsvWriteReadManyLines() throws IOException {
		DelimitedTextStorage storage = new DelimitedTextStorage(testConfig, testDefaultPath, null);

		DelimitedTextEntityWriter writer = new DelimitedTextEntityWriter(storage, testConfig, testDefaultPath,
				DelimitedTextEntityWriter.CSV);
		TestUtils.writeDataAndClose(writer, DATA09ARRAY);

		DelimitedTextEntityReader reader = new DelimitedTextEntityReader(storage, testConfig, testDefaultPath,
				DelimitedTextEntityReader.CSV);
		List<String[]> data = TestUtils.readData(reader);
		assertThat(data.size(), is(1));
		assertThat(data.get(0), arrayContainingInAnyOrder(DATA09ARRAY));
	}

	@Test
	public void testWriteReadManyLinesWithNamingAndRollover() throws IOException {
		DelimitedTextStorage storage = new DelimitedTextStorage(testConfig, testDefaultPath, null);
		storage.setRolloverStrategy(new SizeRolloverStrategy(40));
		storage.setFileNamingStrategy(new RollingFileNamingStrategy());

		TextEntityWriter writer = new TextEntityWriter(storage, testConfig, testDefaultPath);
		TestUtils.writeDataAndClose(writer, DATA09ARRAY);

		DelimitedTextStorage storage1 = new DelimitedTextStorage(testConfig, testDefaultPath, null);
		TextEntityReader reader1 = new TextEntityReader(storage1, testConfig, testDefaultPath.suffix("0"));
		List<String> splitData1 = TestUtils.readData(reader1);

		DelimitedTextStorage storage2 = new DelimitedTextStorage(testConfig, testDefaultPath, null);
		TextEntityReader reader2 = new TextEntityReader(storage2, testConfig, testDefaultPath.suffix("1"));
		List<String> splitData2 = TestUtils.readData(reader2);

		DelimitedTextStorage storage3 = new DelimitedTextStorage(testConfig, testDefaultPath, null);
		TextEntityReader reader3 = new TextEntityReader(storage3, testConfig, testDefaultPath.suffix("2"));
		List<String> splitData3 = TestUtils.readData(reader3);

		assertThat(splitData1.size() + splitData2.size() + splitData3.size(), is(DATA09ARRAY.length));
	}

}
