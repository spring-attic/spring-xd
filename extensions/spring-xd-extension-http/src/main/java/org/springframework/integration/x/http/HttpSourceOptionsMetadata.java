/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.x.http;

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Captures options available to the {@code http} source module.
 */
public class HttpSourceOptionsMetadata {

	private int port = 9000;

	private Class<?> unmarshallTo;


	// The module's PPC conversion service would need a Class->String
	// or bypass the Class->String->Class conversion altogether
	// This will work for now
	public String getUnmarshallTo() {
		return unmarshallTo == null ? null : unmarshallTo.getName();
	}

	@ModuleOption("the type to convert to")
	public void setUnmarshallTo(Class<?> unmarshallTo) {
		this.unmarshallTo = unmarshallTo;
	}

	public int getPort() {
		return port;
	}

	@ModuleOption("the port to listen to")
	public void setPort(int port) {
		this.port = port;
	}


}
