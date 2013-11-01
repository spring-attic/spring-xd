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

package org.springframework.data.hadoop.store.naming;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.junit.Test;

import org.springframework.data.hadoop.store.AbstractStrategiesStorage;
import org.springframework.data.hadoop.store.DataReader;
import org.springframework.data.hadoop.store.DataWriter;
import org.springframework.data.hadoop.store.codec.CodecInfo;
import org.springframework.data.hadoop.store.codec.Codecs;

/**
 * Tests for {@code FileNamingStrategy}s via storage resolved paths.
 * 
 * @author Janne Valkealahti
 * 
 */
@SuppressWarnings("resource")
public class NamingWithStorageTests {

	private final static Path basePath = new Path("/base");

	@Test
	public void testWithChainingStatic() {
		TestStorage storage = new TestStorage(new Configuration(), basePath, null);
		ChainedFileNamingStrategy chained = new ChainedFileNamingStrategy();
		chained.register(new StaticFileNamingStrategy());
		storage.setFileNamingStrategy(chained);
		Path resolvedPath = storage.exposeResolvedPath();
		assertThat(resolvedPath, notNullValue());
		assertThat(resolvedPath.toString(), is("/base/data"));
	}

	@Test
	public void testWithStatic() {
		TestStorage storage = new TestStorage(new Configuration(), basePath, null);
		storage.setFileNamingStrategy(new StaticFileNamingStrategy());
		Path resolvedPath = storage.exposeResolvedPath();
		assertThat(resolvedPath, notNullValue());
		assertThat(resolvedPath.toString(), is("/base/data"));
	}

	@Test
	public void testWithRenaming() {
		TestStorage storage = new TestStorage(new Configuration(), basePath, null);
		storage.setFileNamingStrategy(new RenamingFileNamingStrategy(null, "-suffix", "prefix-"));
		Path resolvedPath = storage.exposeResolvedPath();
		assertThat(resolvedPath, notNullValue());
		assertThat(resolvedPath.toString(), is("/prefix-base-suffix"));
	}

	@Test
	public void testWithChainingRolling() {
		TestStorage storage = new TestStorage(new Configuration(), basePath, null);
		ChainedFileNamingStrategy chained = new ChainedFileNamingStrategy();
		chained.register(new RollingFileNamingStrategy());
		storage.setFileNamingStrategy(chained);
		Path resolvedPath = storage.exposeResolvedPath();
		assertThat(resolvedPath, notNullValue());
		assertThat(resolvedPath.toString(), is("/base0"));
	}

	@Test
	public void testWithRolling() {
		TestStorage storage = new TestStorage(new Configuration(), basePath, null);
		storage.setFileNamingStrategy(new RollingFileNamingStrategy());
		Path resolvedPath = storage.exposeResolvedPath();
		assertThat(resolvedPath, notNullValue());
		assertThat(resolvedPath.toString(), is("/base0"));
	}

	@Test
	public void testWithChainingAll() {
		TestStorage storage = new TestStorage(new Configuration(), basePath, null);
		ChainedFileNamingStrategy chained = new ChainedFileNamingStrategy();
		chained.register(new StaticFileNamingStrategy());
		chained.register(new RollingFileNamingStrategy());
		chained.register(new RenamingFileNamingStrategy());
		storage.setFileNamingStrategy(chained);
		Path resolvedPath = storage.exposeResolvedPath();
		assertThat(resolvedPath, notNullValue());
		assertThat(resolvedPath.toString(), is("/base/data0.tmp"));
	}

	@Test
	public void testWithChainingStaticAndRenaming() {
		TestStorage storage = new TestStorage(new Configuration(), basePath, null);
		ChainedFileNamingStrategy chained = new ChainedFileNamingStrategy();
		chained.register(new StaticFileNamingStrategy());
		chained.register(new RenamingFileNamingStrategy());
		storage.setFileNamingStrategy(chained);
		Path resolvedPath = storage.exposeResolvedPath();
		assertThat(resolvedPath, notNullValue());
		assertThat(resolvedPath.toString(), is("/base/data.tmp"));
	}

	@Test
	public void testWithChainingAllAndCodec() {
		TestStorage storage = new TestStorage(new Configuration(), basePath, Codecs.getCodecInfo("gzip"));
		ChainedFileNamingStrategy chained = new ChainedFileNamingStrategy();
		chained.register(new StaticFileNamingStrategy());
		chained.register(new RollingFileNamingStrategy());
		chained.register(new CodecFileNamingStrategy());
		chained.register(new RenamingFileNamingStrategy());
		storage.setFileNamingStrategy(chained);
		Path resolvedPath = storage.exposeResolvedPath();
		assertThat(resolvedPath, notNullValue());
		assertThat(resolvedPath.toString(), is("/base/data0.gzip.tmp"));
	}

	/**
	 * Simple mock to test naming integration.
	 */
	private static class TestStorage extends AbstractStrategiesStorage {

		public TestStorage(Configuration configuration, Path basePath, CodecInfo codec) {
			super(configuration, basePath, codec);
		}

		@Override
		public DataWriter getDataWriter() throws IOException {
			throw new UnsupportedOperationException("not used");
		}

		@Override
		public DataReader getDataReader(Path path) throws IOException {
			throw new UnsupportedOperationException("not used");
		}

		public Path exposeResolvedPath() {
			return getResolvedPath();
		}

	}

}
