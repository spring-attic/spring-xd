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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.io.SequenceFile.Writer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.util.ReflectionUtils;

import org.springframework.data.hadoop.store2.codec.CodecInfo;
import org.springframework.data.hadoop.store2.support.StoreObjectSupport;
import org.springframework.util.ClassUtils;

public abstract class AbstractSequenceFileWriter extends StoreObjectSupport {

	public AbstractSequenceFileWriter(Configuration configuration, Path basePath, CodecInfo codec) {
		super(configuration, basePath, codec);
	}

	protected Writer getOutput() throws IOException {

		FileSystem fs = FileSystem.get(getConfiguration());

		Writer writer;
		CodecInfo codecInfo = getCodec();
		if (codecInfo == null) {
			writer = SequenceFile.createWriter(
					fs, getConfiguration(), getResolvedPath(),
					Text.class, Text.class, CompressionType.NONE, (CompressionCodec) null);
		}
		else {
			Class<?> clazz = ClassUtils.resolveClassName(codecInfo.getCodecClass(), getClass().getClassLoader());
			CompressionCodec compressionCodec = (CompressionCodec) ReflectionUtils.newInstance(clazz,
					getConfiguration());
			writer = SequenceFile.createWriter(fs,
					getConfiguration(), getResolvedPath(),
					Text.class, Text.class, CompressionType.RECORD, compressionCodec);
		}

		return writer;
	}
}
