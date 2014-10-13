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

package org.springframework.xd.dirt.cluster;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.Assert;

/**
 * Runtime metadata for a Admin server instance.
 *
 * @author Mark Fisher
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 */
public class AdminAttributes implements Map<String, String>, Comparable<AdminAttributes> {

	public final static String ADMIN_ID_KEY = "id";

	public final static String HOST_KEY = "host";

	public final static String PORT_KEY = "port";

	public final static String PROCESS_ID_KEY = "pid";

	public final static String IP_ADDRESS_KEY = "ip";

	public final static String MGMT_PORT_KEY = "managementPort";

	private final List<String> commonAttributeKeys = Arrays.asList(ADMIN_ID_KEY, PROCESS_ID_KEY,
			HOST_KEY, PORT_KEY, IP_ADDRESS_KEY);

	/**
	 * The underlying map.
	 */
	private final Map<String, String> map = new ConcurrentHashMap<String, String>();

	/**
	 * Default constructor generates a random id.
	 */
	public AdminAttributes() {
		this(UUID.randomUUID().toString());
	}

	/**
	 * Constructor to be called when the id is already known.
	 *
	 * @param id the admin server's id
	 */
	public AdminAttributes(String id) {
		Assert.hasText(id, "id is required");
		this.put(ADMIN_ID_KEY, id);
	}

	/**
	 * Constructor to be called when the attributes are already known. If an id is not present in the map, one will be
	 * generated.
	 *
	 * @param attributes the admin server's attributes
	 */
	public AdminAttributes(Map<? extends String, ? extends String> attributes) {
		Assert.notNull(attributes, "attributes must not be null");
		if (!attributes.containsKey(ADMIN_ID_KEY)) {
			this.put(ADMIN_ID_KEY, UUID.randomUUID().toString());
		}
		this.putAll(attributes);
	}

	public String getId() {
		return this.get(ADMIN_ID_KEY);
	}

	public String getHost() {
		return this.get(HOST_KEY);
	}

	public int getPort() {
		return Integer.parseInt(this.get(PORT_KEY));
	}

	public String getIp() {
		return this.get(IP_ADDRESS_KEY);
	}

	public int getManagementPort() {
		return Integer.parseInt(this.get(MGMT_PORT_KEY));
	}

	public int getPid() {
		return Integer.parseInt(this.get(PROCESS_ID_KEY));
	}

	public AdminAttributes setPid(Integer pid) {
		this.put(PROCESS_ID_KEY, String.valueOf(pid));
		return this;
	}

	public AdminAttributes setHost(String host) {
		this.put(HOST_KEY, host);
		return this;
	}

	public AdminAttributes setPort(Integer port) {
		this.put(PORT_KEY, String.valueOf(port));
		return this;
	}

	public AdminAttributes setIp(String ip) {
		this.put(IP_ADDRESS_KEY, ip);
		return this;
	}

	public AdminAttributes setManagementPort(Integer mgmtPort) {
		this.put(MGMT_PORT_KEY, String.valueOf(mgmtPort));
		return this;
	}

	/**
	 * Retrieve the custom attributes for this admin. This will not include the values
	 * for common keys: {@value #ADMIN_ID_KEY}, {@value #PROCESS_ID_KEY}, {@value #HOST_KEY},
	 * and {@value #IP_ADDRESS_KEY}.
	 *
	 * @return the map of custom attributes
	 */
	public Map<String, String> getCustomAttributes() {
		Map<String, String> customAttributes = new HashMap<String, String>();
		for (String key : this.keySet()) {
			if (!commonAttributeKeys.contains(key)) {
				customAttributes.put(key, this.get(key));
			}
		}
		return customAttributes;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return map.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String get(Object key) {
		return map.get(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String put(String key, String value) {
		return map.put(key, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String remove(Object key) {
		return map.remove(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void putAll(Map<? extends String, ? extends String> m) {
		map.putAll(m);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		this.map.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> keySet() {
		return Collections.unmodifiableSet(map.keySet());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<String> values() {
		return Collections.unmodifiableCollection(map.values());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<java.util.Map.Entry<String, String>> entrySet() {
		return Collections.unmodifiableSet(map.entrySet());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return map.toString();
	}

	/**
	 * Compares using the adminId.
	 */
	@Override
	public int compareTo(AdminAttributes other) {
		return this.getId().compareTo(other.getId());
	}

}
