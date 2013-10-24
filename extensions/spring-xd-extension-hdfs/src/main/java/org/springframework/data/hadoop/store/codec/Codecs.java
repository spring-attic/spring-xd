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

package org.springframework.data.hadoop.store.codec;

import org.apache.hadoop.io.compress.BZip2Codec;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.io.compress.SnappyCodec;

/**
 * Default codecs supported by {@link Storage} framework. We keep codec info here for implementations which are
 * supported out of the box. Reference to codec is a fully qualified name of a class, not a class itself. This allows
 * user to define and use codecs which are added into a classpath unknown during the compilation time.
 * 
 * @author Janne Valkealahti
 * 
 */
public enum Codecs {

	/**
	 * Non-splittable {@link GzipCodec}.
	 */
	GZIP(new DefaultCodecInfo(GzipCodec.class.getName(), false)),

	/**
	 * Non-splittable {@link SnappyCodec}.
	 */
	SNAPPY(new DefaultCodecInfo(SnappyCodec.class.getName(), false)),

	/**
	 * Splittable {@link BZip2Codec}.
	 */
	BZIP2(new DefaultCodecInfo(BZip2Codec.class.getName(), true));

	private final CodecInfo codec;

	/**
	 * Instantiates a new codecs.
	 * 
	 * @param codec the codec info
	 */
	private Codecs(CodecInfo codec) {
		this.codec = codec;
	}

	/**
	 * Gets the codec info.
	 * 
	 * @return the codec info
	 */
	public CodecInfo getCodecInfo() {
		return codec;
	}

}
