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

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.hadoop.store.input.TextEntityReader;
import org.springframework.data.hadoop.store.naming.ChainedFileNamingStrategy;
import org.springframework.data.hadoop.store.output.TextEntityWriter;
import org.springframework.data.hadoop.store.text.DelimitedTextStorage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class DelimitedTextStorageCtxTests extends AbstractDataTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testWriteReadManyLines() throws IOException, InterruptedException {

		DelimitedTextStorage storage = context.getBean("storage", DelimitedTextStorage.class);
		// Configuration configuration = context.getBean("hadoopConfiguration", Configuration.class);
		assertNotNull(storage);

		ChainedFileNamingStrategy fileNamingStrategy = context.getBean("chainedFileNamingStrategy",
				ChainedFileNamingStrategy.class);
		assertNotNull(fileNamingStrategy);
		storage.setFileNamingStrategy(fileNamingStrategy);

		TextEntityWriter writer = context.getBean("writer", TextEntityWriter.class);
		assertNotNull(writer);

		TextEntityReader reader = context.getBean("reader", TextEntityReader.class);
		assertNotNull(reader);

		TestUtils.writeDataAndClose(writer, DATA09ARRAY);
		TestUtils.readDataAndAssert(reader, DATA09ARRAY);
	}

}
