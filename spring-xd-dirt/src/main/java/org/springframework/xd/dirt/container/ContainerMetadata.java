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
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.springframework.util.Assert;

/**
 * Metadata for a Container instance.
 * 
 * @author Mark Fisher
 */
public class ContainerMetadata {

	/**
	 * The container's id.
	 */
	private final String id;

	/**
	 * The container's process id.
	 */
	private Integer pid;

	/**
	 * The container's host name.
	 */
	private String host;

	/**
	 * The container's IP address.
	 */
	private String ip;

	/**
	 * The set of groups this container belongs to.
	 */
	private final Set<String> groups;

	/**
	 * Default constructor generates a random id.
	 */
	public ContainerMetadata() {
		this(UUID.randomUUID().toString(), null, null, null);
	}

	/**
	 * Constructor to be called when the metadata is already known (i.e. when deserializing from the repository).
	 * 
	 * @param id the container's id
	 * @param pid the container's pid
	 * @param host the container's host
	 * @param ip the container's ip
	 */
	// todo: replace with a builder; too many string args, brittle
	public ContainerMetadata(String id, Integer pid, String host, String ip) {
		Assert.hasText(id, "id is required");
		this.id = id;
		this.pid = pid;
		this.host = host;
		this.ip = ip;
		// todo: support groups (see the ctor for ContainerServer in xdzk)
		this.groups = Collections.emptySet();
	}

	public String getId() {
		return this.id;
	}

	public String getHost() {
		if (this.host == null) {
			try {
				this.host = InetAddress.getLocalHost().getHostName();
			}
			catch (UnknownHostException uhe) {
				this.host = "unknown";
			}
		}
		return this.host;
	}

	public String getIp() {
		if (this.ip == null) {
			try {
				this.ip = InetAddress.getLocalHost().getHostAddress();
			}
			catch (UnknownHostException uhe) {
				this.ip = "unknown";
			}
		}
		return this.ip;
	}

	public int getPid() {
		if (this.pid == null) {
			String jvmName = ManagementFactory.getRuntimeMXBean().getName();
			this.pid = jvmName.indexOf('@') != -1 ? Integer.parseInt(jvmName.split("@")[0]) : -1;
		}
		return this.pid;
	}

	public Set<String> getGroups() {
		return Collections.unmodifiableSet(groups);
	}

	@Override
	public String toString() {
		return "[id=" + id + ", pid=" + pid + ", host=" + host + ", ip=" + ip + ", groups=" + groups + "]";
	}

}
