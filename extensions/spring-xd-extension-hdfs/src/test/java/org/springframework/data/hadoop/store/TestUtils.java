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

package org.springframework.data.hadoop.store;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import org.springframework.data.hadoop.store.input.TextEntityReader;
import org.springframework.data.hadoop.store.output.DelimitedTextEntityWriter;
import org.springframework.data.hadoop.store.output.TextEntityWriter;

/**
 * Testing utilities.
 * 
 * @author Janne Valkealahti
 * 
 */
public abstract class TestUtils {

	/**
	 * Read field from a given object.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T readField(String name, Object target) throws Exception {
		Field field = null;
		Class<?> clazz = target.getClass();
		do {
			try {
				field = clazz.getDeclaredField(name);
			}
			catch (Exception ex) {
			}

			clazz = clazz.getSuperclass();
		}
		while (field == null && !clazz.equals(Object.class));

		if (field == null)
			throw new IllegalArgumentException("Cannot find field '" + name + "' in the class hierarchy of "
					+ target.getClass());
		field.setAccessible(true);
		return (T) field.get(target);
	}

	/**
	 * Write data into {@code TextDataWriter}. Data items are strings starting from zero and counting up to given count.
	 */
	public static void writeDataAndClose(TextEntityWriter writer, int count) throws IOException {
		writer.open();
		for (int i = 0; i < count; i++) {
			writer.write(Integer.toString(i));
		}
		writer.flush();
		writer.close();
	}

	/**
	 * Write data into {@code TextDataWriter}. Data items are strings in a given array.
	 */
	public static void writeDataAndClose(TextEntityWriter writer, String[] data) throws IOException {
		writer.open();
		for (String line : data) {
			writer.write(line);
		}
		writer.flush();
		writer.close();
	}

	/**
	 * Write data into {@code DelimitedTextDataWriter}. Data items are strings in a given array.
	 */
	public static void writeDataAndClose(DelimitedTextEntityWriter writer, String[] data) throws IOException {
		writer.open();
		writer.write(data);
		writer.flush();
		writer.close();
	}

	/**
	 * Read data from {@code TextDataReader}. Asserts read items with a given array in a specific order.
	 */
	public static void readDataAndAssert(TextEntityReader reader, String[] expected) throws IOException {
		String line = null;
		int count = 0;
		while ((line = reader.read()) != null) {
			assertThat(count, lessThan(expected.length));
			assertThat(line, is(expected[count++]));
		}
		assertThat(count, is(expected.length));
		line = reader.read();
		assertNull("Expected null, got '" + line + "'", line);
	}

	/**
	 * Read data from {@code TextDataReader} and returns items in a list.
	 */
	public static List<String> readData(TextEntityReader reader) throws IOException {
		ArrayList<String> ret = new ArrayList<String>();
		String line = null;
		while ((line = reader.read()) != null) {
			ret.add(line);
		}
		return ret;
	}

	/**
	 * Gets file length.
	 */
	public static long getFileLength(Path path, Configuration configuration) throws IOException {
		return path.getFileSystem(configuration).getFileStatus(path).getLen();
	}


}
