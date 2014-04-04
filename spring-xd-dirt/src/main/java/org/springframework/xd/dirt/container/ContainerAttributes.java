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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Metadata for a Container instance.
 *
 * @author Mark Fisher
 * @author David Turanski
 */
public class ContainerAttributes extends LinkedHashMap<String, String> {

	/**
	 * Default constructor generates a random id.
	 */
	public ContainerAttributes() {
		this(UUID.randomUUID().toString());
	}

	/**
	 * Constructor to be called when the id is already known
	 *
	 * @param id the container's id
	 */
	public ContainerAttributes(String id) {
		Assert.hasText(id, "id is required");
		this.put("id", id);
	}

	public ContainerAttributes(Map<? extends String, ? extends String> attributes) {
		this.putAll(attributes);
	}


	public String getId() {
		return this.get("id");
	}

	public String getHost() {
		return this.get("host");
	}

	public String getIp() {
		return this.get("ip");
	}

	public int getPid() {
		return Integer.parseInt(this.get("pid"));
	}

	public Set<String> getGroups() {
		Set<String> groupSet = new HashSet<String>();
		String groups = this.get("groups");
		groupSet = StringUtils.hasText(groups) ? StringUtils.commaDelimitedListToSet(groups) : new HashSet<String>();
		return Collections.unmodifiableSet(groupSet);
	}

	public ContainerAttributes setPid(Integer pid) {
		this.put("pid", String.valueOf(pid));
		return this;
	}

	public ContainerAttributes setHost(String host) {
		this.put("host", host);
		return this;
	}


	public ContainerAttributes setIp(String ip) {
		this.put("ip", ip);
		return this;
	}

}
