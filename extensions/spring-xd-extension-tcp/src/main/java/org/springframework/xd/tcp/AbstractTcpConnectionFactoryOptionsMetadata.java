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

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Base class for options metadata that deal with a tcp connection factory in some way.
 * 
 * @author Eric Bottard
 */
public abstract class AbstractTcpConnectionFactoryOptionsMetadata {

	private boolean reverseLookup = false;

	private int socketTimeout = 120000;

	private boolean nio = false;

	private boolean useDirectBuffers = false;

	public boolean isUseDirectBuffers() {
		return useDirectBuffers;
	}

	@ModuleOption("whether or not to use direct buffers")
	public void setUseDirectBuffers(boolean useDirectBuffers) {
		this.useDirectBuffers = useDirectBuffers;
	}

	public boolean isNio() {
		return nio;
	}

	@ModuleOption("whether or not to use NIO")
	public void setNio(boolean nio) {
		this.nio = nio;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	@ModuleOption("the timeout (ms) before closing the socket when no data is received")
	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public boolean isReverseLookup() {
		return reverseLookup;
	}

	@ModuleOption("perform a reverse DNS lookup on the remote IP Address")
	public void setReverseLookup(boolean reverseLookup) {
		this.reverseLookup = reverseLookup;
	}


}
