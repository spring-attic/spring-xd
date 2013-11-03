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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CodecPool;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.Decompressor;
import org.apache.hadoop.util.ReflectionUtils;

import org.springframework.data.hadoop.store.codec.CodecInfo;
import org.springframework.data.hadoop.store.support.LifecycleObjectSupport;
import org.springframework.data.hadoop.store.support.StreamsHolder;
import org.springframework.util.ClassUtils;

/**
 * Base implementation of a {@code Storage} sharing a common functionality among storage formats.
 * 
 * @author Janne Valkealahti
 * 
 */
public abstract class AbstractStorage extends LifecycleObjectSupport implements Storage {

	private final static Log log = LogFactory.getLog(AbstractStorage.class);

	/** Hadoop configuration */
	private final Configuration configuration;

	/** Codec info for storage */
	private final CodecInfo codecInfo;

	/** Hdfs path into a storage */
	private final Path basePath;

	/**
	 * Instantiates a new abstract storage format.
	 * 
	 * @param configuration the hadoop configuration
	 * @param basePath the hdfs path
	 * @param codec the compression codec info
	 */
	public AbstractStorage(Configuration configuration, Path basePath, CodecInfo codec) {
		this.configuration = configuration;
		this.basePath = basePath;
		this.codecInfo = codec;
	}

	/**
	 * Gets the configuration.
	 * 
	 * @return the configuration
	 */
	public Configuration getConfiguration() {
		return configuration;
	}

	/**
	 * Gets the path.
	 * 
	 * @return the path
	 */
	public Path getPath() {
		return basePath;
	}

	/**
	 * Gets the codec.
	 * 
	 * @return the codec
	 */
	public CodecInfo getCodec() {
		return codecInfo;
	}

	/**
	 * Checks if is compressed.
	 * 
	 * @return true, if is compressed
	 */
	public boolean isCompressed() {
		return codecInfo != null;
	}

	/**
	 * Creates the needed output streams for the storage.
	 * 
	 * @return the stream holder
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected StreamsHolder<OutputStream> getOutput() throws IOException {
		StreamsHolder<OutputStream> holder = new StreamsHolder<OutputStream>();
		FileSystem fs = FileSystem.get(getConfiguration());
		Path p = getResolvedPath();
		log.info("Creating output for path " + p);
		holder.setPath(p);
		if (!isCompressed()) {
			OutputStream out = fs.create(p);
			holder.setStream(out);
		}
		else {
			Class<?> clazz = ClassUtils.resolveClassName(codecInfo.getCodecClass(), getClass().getClassLoader());
			CompressionCodec compressionCodec = (CompressionCodec) ReflectionUtils.newInstance(clazz,
					getConfiguration());
			FSDataOutputStream wout = fs.create(p);
			OutputStream out = compressionCodec.createOutputStream(wout);
			holder.setWrappedStream(wout);
			holder.setStream(out);
		}
		return holder;
	}

	/**
	 * Gets the resolved path.
	 * 
	 * @return the resolved path
	 */
	protected Path getResolvedPath() {
		return getPath();
	}

	/**
	 * Creates the needed input streams for the storage.
	 * 
	 * @return the stream holder
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected StreamsHolder<InputStream> getInput(Path inputPath) throws IOException {
		log.info("Creating new InputStream");
		StreamsHolder<InputStream> holder = new StreamsHolder<InputStream>();
		final FileSystem fs = basePath.getFileSystem(configuration);
		Path p = inputPath.isAbsolute() ? inputPath : new Path(getPath(), inputPath);
		if (!fs.exists(p)) {
			throw new StorageException("Path " + p + " does not exist");
		}
		if (!isCompressed()) {
			InputStream input = fs.open(p);
			holder.setStream(input);
		}
		else {
			Class<?> clazz = ClassUtils.resolveClassName(codecInfo.getCodecClass(), getClass().getClassLoader());
			CompressionCodec compressionCodec = (CompressionCodec) ReflectionUtils.newInstance(clazz,
					getConfiguration());
			Decompressor decompressor = CodecPool.getDecompressor(compressionCodec);
			FSDataInputStream winput = fs.open(p);
			InputStream input = compressionCodec.createInputStream(winput, decompressor);
			holder.setWrappedStream(winput);
			holder.setStream(input);
		}
		return holder;
	}

}
