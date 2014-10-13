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

import java.util.Map;

import org.springframework.util.Assert;

/**
 * Domain object for an XD admin runtime. This object is typically constructed
 * from admin runtime data maintained in ZooKeeper.
 *
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 */
public class Admin implements Comparable<Admin> {

	/**
	 * Admin name.
	 */
	private final String name;

	/**
	 * Admin attributes.
	 */
	private final AdminAttributes attributes;

	/**
	 * Construct a Container object.
	 *
	 * @param name        admin name
	 * @param attributes  admin attributes
	 */
	public Admin(String name, Map<String, String> attributes) {
		Assert.hasText(name);
		this.name = name;
		this.attributes = new AdminAttributes(attributes);
	}

	/**
	 * Return the admin name.
	 *
	 * @return admin name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the {@link AdminAttributes}.
	 *
	 * @return read-only map of admin attributes
	 */
	public AdminAttributes getAttributes() {
		return attributes;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(Admin other) {
		return this.getName().compareTo(other.getName());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o) {
		return o instanceof Admin && name.equals(((Admin) o).getName());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return name.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "Admin{" +
				"name='" + name + '\'' +
				", attributes=" + attributes +
				'}';
	}
}
