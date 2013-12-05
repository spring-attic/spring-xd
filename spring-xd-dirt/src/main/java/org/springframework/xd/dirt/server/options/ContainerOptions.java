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

package org.springframework.xd.dirt.server.options;

import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * Holds configuration options that are valid for the Container node, when using distributed mode.
 * 
 * @author Eric Bottard
 * @author David Turanski
 */
@ConfigurationProperties
public class ContainerOptions extends CommonDistributedOptions {

	public static enum DataTransport {
		rabbit, redis
	}

	private DataTransport transport;

	public void setXD_TRANSPORT(DataTransport transport) {
		this.transport = transport;
	}

	public DataTransport getXD_TRANSPORT() {
		return this.transport;
	}


}
