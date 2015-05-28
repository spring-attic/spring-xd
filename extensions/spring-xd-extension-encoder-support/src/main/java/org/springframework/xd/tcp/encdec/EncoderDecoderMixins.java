/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.tcp.encdec;

import static org.springframework.xd.tcp.encdec.EncoderDecoderMixins.Encoding.CRLF;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;


/**
 * Provides mixins for encoder/decoder options.
 * 
 * <p>
 * Implemented as separate classes as the bufferSize option may be used by both.
 * 
 * @author Eric Bottard
 */
public final class EncoderDecoderMixins {

	private EncoderDecoderMixins() {

	}

	public static enum Encoding {
		CRLF, LF, NULL, STXETX, RAW, L1, L2, L4;
	}

	/**
	 * Adds an {@code encoder} option (default CRLF) and activates a profile named after the chosen encoder.
	 * 
	 * @author Eric Bottard
	 */
	public static class EncoderMixin implements ProfileNamesProvider {

		private Encoding encoder = CRLF;

		@NotNull
		public Encoding getEncoder() {
			return encoder;
		}

		@ModuleOption("the encoder to use when sending messages")
		public void setEncoder(Encoding encoder) {
			this.encoder = encoder;
		}


		@Override
		public String[] profilesToActivate() {
			return new String[] { "use-" + encoder.name().toLowerCase() };
		}

	}

	/**
	 * Adds an {@code decoder} option (default CRLF) and activates a profile named after the chosen decoder.
	 * 
	 * @author Eric Bottard
	 */
	public static class DecoderMixin implements ProfileNamesProvider {

		private Encoding decoder = CRLF;

		@NotNull
		public Encoding getDecoder() {
			return decoder;
		}

		@ModuleOption("the decoder to use when receiving messages")
		public void setDecoder(Encoding decoder) {
			this.decoder = decoder;
		}


		@Override
		public String[] profilesToActivate() {
			return new String[] { "use-" + decoder.name().toLowerCase() };
		}

	}

	/**
	 * Adds a {@code bufferSize} option (default 2048), to be used alongside a decoder/encoder (or both) option.
	 * 
	 * @author Eric Bottard
	 */
	public static class BufferSizeMixin {

		private int bufferSize = 2048;

		@Min(0)
		public int getBufferSize() {
			return bufferSize;
		}

		@ModuleOption("the size of the buffer (bytes) to use when encoding/decoding")
		public void setBufferSize(int bufferSize) {
			this.bufferSize = bufferSize;
		}


	}

}
