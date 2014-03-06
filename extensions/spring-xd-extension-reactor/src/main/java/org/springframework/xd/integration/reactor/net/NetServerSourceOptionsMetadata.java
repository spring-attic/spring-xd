/*
 * Copyright 2013-2014 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.xd.integration.reactor.net;

import javax.validation.constraints.NotNull;

import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Provides metadata about the configuration options of a {@link reactor.net.NetServer} in Spring XD.
 * 
 * @author Jon Brisbin
 */
public class NetServerSourceOptionsMetadata {

	private String bind = "tcp://0.0.0.0:3000/linefeed?codec=string";

	@NotNull
	public String getBind() {
		return bind;
	}

	@ModuleOption("URI which configures the NetServer")
	public void setBind(String bind) {
		this.bind = bind;
	}

}
