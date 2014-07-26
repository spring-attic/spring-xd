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

package org.springframework.xd.dirt.util;

/**
 * Static constants for configuration locations.
 * 
 * @author Mark Fisher
 */
public class ConfigLocations {

	/**
	 * Base location for XD config files. Chosen so as not to collide with user provided content.
	 */
	public static final String XD_CONFIG_ROOT = "META-INF/spring-xd/";

	/**
	 * Where container related config files reside.
	 */
	public static final String XD_INTERNAL_CONFIG_ROOT = XD_CONFIG_ROOT + "internal/";

	/**
	 * Where batch related config files reside.
	 */
	public static final String XD_BATCH_CONFIG_ROOT = XD_CONFIG_ROOT + "batch/";

	public static final String XD_ADMIN_UI_BASE_PATH = "admin-ui";

	/**
	 * Prevent instantiation.
	 */
	private ConfigLocations() {
	}

}
