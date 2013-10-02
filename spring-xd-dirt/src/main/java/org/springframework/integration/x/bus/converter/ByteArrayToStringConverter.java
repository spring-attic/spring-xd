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

package org.springframework.integration.x.bus.converter;

import java.nio.charset.Charset;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;


/**
 * Converts a byte[] to String using the provided Charset (UTF-8 by default)
 * 
 * @author David Turanski
 * @since 1.0
 */
public class ByteArrayToStringConverter implements Converter<byte[], String>, Cloneable {

	private final Charset charset;

	public ByteArrayToStringConverter() {
		charset = Charset.forName("UTF-8");
	}

	/**
	 * Constructor accepting a charset name
	 * 
	 * @param charsetName must correspond to a supported Charset else an exception will be thrown
	 */
	public ByteArrayToStringConverter(String charsetName) {
		Assert.hasText(charsetName, "'charsetName' cannot be empty or null");
		Assert.isTrue(Charset.isSupported(charsetName), charsetName + " is not a supported Charset");
		charset = Charset.forName(charsetName);
	}

	@Override
	public String convert(byte[] source) {
		return new String(source, charset);
	}
}
