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

package org.springframework.xd.tcp;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.tcp.encdec.EncoderDecoderMixins.BufferSizeMixin;
import org.springframework.xd.tcp.encdec.EncoderDecoderMixins.DecoderMixin;


/**
 * Mixin for options metadata that configure a server tcp connection factory.
 * 
 * @author Eric Bottard
 */
@Mixin({ DecoderMixin.class, BufferSizeMixin.class })
public class ServerTcpConnectionFactoryOptionsMetadataMixin extends AbstractTcpConnectionFactoryOptionsMetadata {

	private int port = 1234;

	@Min(0)
	@Max(65536)
	public int getPort() {
		return port;
	}

	@ModuleOption("the port on which to listen")
	public void setPort(int port) {
		this.port = port;
	}


}
