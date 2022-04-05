/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.tcp;

import javax.validation.constraints.Min;

import org.springframework.xd.module.options.mixins.ExpressionOrScriptMixin;
import org.springframework.xd.module.options.mixins.MaxMessagesDefaultOneMixin;
import org.springframework.xd.module.options.mixins.ToStringCharsetMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.tcp.encdec.EncoderDecoderMixins.DecoderMixin;


/**
 * Captures options available to the {@code tcp-client} source module.
 *
 * @author Eric Bottard
 */
@Mixin({ ExpressionOrScriptMixin.class,
	ClientTcpConnectionFactoryOptionsMetadataMixin.class,
	DecoderMixin.class,
	ToStringCharsetMixin.class,
	MaxMessagesDefaultOneMixin.class })
public class TcpClientSourceOptionsMetadata {

	private int fixedDelay = 5;


	@Min(1)
	public int getFixedDelay() {
		return fixedDelay;
	}

	@ModuleOption("the rate at which stimulus messages will be emitted (seconds)")
	public void setFixedDelay(int fixedDelay) {
		this.fixedDelay = fixedDelay;
	}

}
