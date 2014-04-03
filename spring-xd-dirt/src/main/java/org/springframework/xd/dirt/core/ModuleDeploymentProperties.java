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

package org.springframework.xd.dirt.core;

import java.util.HashMap;

/**
 * Deployment properties for a module.
 *
 * @author Mark Fisher
 */
public class ModuleDeploymentProperties extends HashMap<String, String> {

	private static final String TARGET = "target";

	public static final String COUNT = "count";

	public static final String TARGET_GROUP = TARGET + ".group";

	public ModuleDeploymentProperties() {
		put(COUNT, "" + 1);
	}

	/**
	 * Return the number of container instances this module should be deployed to. A value of 0 indicates that this
	 * module should be deployed to all containers in the {@link #group}. If {@code group} is null and the value is 0,
	 * this module should be deployed to all containers.
	 *
	 * @return number of container instances
	 */
	public int getCount() {
		return parseCount(get(COUNT));
	}

	/**
	 * Specify the number of container instances this module should be deployed to. A value of 0 indicates that this module should
	 * be deployed to all containers in the {@link #group}. If {@code group} is null and the value is 0, this module
	 * should be deployed to all containers.
	 */
	public ModuleDeploymentProperties setCount(int count) {
		put(COUNT, "" + count);
		return this;
	}

	/**
	 * Return the group of containers this module should be deployed to.
	 *
	 * @return container group name or {@code null} if no group was specified.
	 */
	public String getTargetGroup() {
		return get(TARGET_GROUP);
	}

	/**
	 * Specify the group of containers this module should be deployed to.
	 */
	public ModuleDeploymentProperties setTargetGroup(String targetGroup) {
		put(TARGET_GROUP, targetGroup);
		return this;
	}

	/**
	 * Convert a String value to an int. Returns 1 if the String is null.
	 *
	 * @param s string to convert
	 *
	 * @return int value of String, or 1 if null
	 */
	private int parseCount(String s) {
		return s == null ? 1 : Integer.valueOf(s);
	}

}
