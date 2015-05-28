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
import javax.validation.constraints.NotNull;

import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.tcp.encdec.EncoderDecoderMixins.BufferSizeMixin;
import org.springframework.xd.tcp.encdec.EncoderDecoderMixins.EncoderMixin;


/**
 * Mixin for options metadata that configure a client tcp connection factory.
 * 
 * @author Eric Bottard
 */
@Mixin({ EncoderMixin.class, BufferSizeMixin.class })
public class ClientTcpConnectionFactoryOptionsMetadataMixin extends AbstractTcpConnectionFactoryOptionsMetadata {

	private boolean close;

	private String host = "localhost";

	private int port = 1234;


	@NotNull
	public String getHost() {
		return host;
	}

	@Min(0)
	@Max(65535)
	public int getPort() {
		return port;
	}


	public boolean isClose() {
		return close;
	}

	@ModuleOption("whether to close the socket after each message")
	public void setClose(boolean close) {
		this.close = close;
	}

	@ModuleOption("the remote host to connect to")
	public void setHost(String host) {
		this.host = host;
	}

	@ModuleOption("the port on the remote host to connect to")
	public void setPort(int port) {
		this.port = port;
	}


}
