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

package org.springframework.xd.module.options.mixins;

import javax.validation.constraints.NotNull;

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * A standard mixin for modules that do conversion from String to bytes.
 * 
 * <p>
 * Exposes the following options:
 * <ul>
 * <li>charset (UTF-8)</li>
 * </ul>
 * 
 * @author Eric Bottard
 */
public class FromStringCharsetMixin {

	private String charset = "UTF-8";

	@NotNull
	public String getCharset() {
		return charset;
	}

	@ModuleOption("the charset used when converting from String to bytes")
	public void setCharset(String charset) {
		this.charset = charset;
	}


}
