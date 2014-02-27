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

package org.springframework.xd.dirt.container;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Metadata for a Container instance.
 * 
 * @author Mark Fisher
 */
public class ContainerMetadata {

	private final String id;

	private String hostName;

	private String ipAddress;

	private String jvmName;

	/**
	 * Default constructor generates a random id.
	 */
	public ContainerMetadata() {
		this.id = UUID.randomUUID().toString();
	}

	public String getId() {
		return this.id;
	}

	public String getHostName() {
		try {
			this.hostName = InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException uhe) {
			this.hostName = "unknown";
		}
		return this.hostName;
	}

	public String getIpAddress() {
		try {
			this.ipAddress = InetAddress.getLocalHost().getHostAddress();
		}
		catch (UnknownHostException uhe) {
			this.ipAddress = "unknown";
		}
		return this.ipAddress;
	}

	public String getJvmName() {
		synchronized (this) {
			if (this.jvmName == null) {
				this.jvmName = ManagementFactory.getRuntimeMXBean().getName();
			}
		}
		return this.jvmName;
	}

}
