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

package org.springframework.data.hadoop.store2.output;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.util.ReflectionUtils;

import org.springframework.data.hadoop.store.support.StreamsHolder;
import org.springframework.data.hadoop.store2.codec.CodecInfo;
import org.springframework.data.hadoop.store2.support.StoreObjectSupport;
import org.springframework.util.ClassUtils;

public abstract class AbstractDataStreamWriter extends StoreObjectSupport {

	private final static Log log = LogFactory.getLog(AbstractDataStreamWriter.class);

	public AbstractDataStreamWriter(Configuration configuration, Path basePath, CodecInfo codec) {
		super(configuration, basePath, codec);
	}

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
			// TODO: will isCompressed() really guard for npe against getCodec()
			Class<?> clazz = ClassUtils.resolveClassName(getCodec().getCodecClass(), getClass().getClassLoader());
			CompressionCodec compressionCodec = (CompressionCodec) ReflectionUtils.newInstance(clazz,
					getConfiguration());
			FSDataOutputStream wout = fs.create(p);
			OutputStream out = compressionCodec.createOutputStream(wout);
			holder.setWrappedStream(wout);
			holder.setStream(out);
		}
		return holder;
	}

}
