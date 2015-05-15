/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.xd.greenplum.gpfdist;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import reactor.fn.Consumer;
import reactor.fn.Function;
import reactor.io.buffer.Buffer;
import reactor.io.codec.Codec;

public class GPFDistCodec extends Codec<Buffer, Buffer, Buffer> {

	final byte[] h1 = Character.toString('D').getBytes(Charset.forName("UTF-8"));

	@SuppressWarnings("resource")
	@Override
	public Buffer apply(Buffer t) {
			byte[] h2 = ByteBuffer.allocate(4).putInt(t.flip().remaining()).array();
			return new Buffer().append(h1).append(h2).append(t).flip();
	}

	@Override
	public Function<Buffer, Buffer> decoder(Consumer<Buffer> next) {
		return null;
	}

}
