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

package org.springframework.xd.module.options.spi;


/**
 * Defines constants for well known XD keys that are available at runtime for a module. These can be used inside default
 * values. Module authors are typically interested in the constants that include placeholder prefixes and suffixes.
 * 
 * @author Eric Bottard
 */
public final class ModulePlaceholders {

	public static final String XD_STREAM_NAME_KEY = "xd.stream.name";

	public static final String XD_JOB_NAME_KEY = "xd.job.name";

	public static final String XD_MODULE_NAME_KEY = "xd.module.name";

	public static final String XD_MODULE_TYPE_KEY = "xd.module.type";

	public static final String XD_MODULE_LABEL_KEY = "xd.module.label";

	public static final String XD_MODULE_INDEX_KEY = "xd.module.index";

	public static final String XD_MODULE_COUNT_KEY = "xd.module.count";

	public static final String XD_MODULE_SEQUENCE_KEY = "xd.module.sequence";

	public static final String XD_CONTAINER_KEY_PREFIX = "xd.container.";


	/**
	 * Will be resolved to the name of the stream the module lives in.
	 */
	public static final String XD_STREAM_NAME = "${" + XD_STREAM_NAME_KEY + "}";

	/**
	 * Will be resolved to the name of the job the module lives in.
	 */
	public static final String XD_JOB_NAME = "${" + XD_JOB_NAME_KEY + "}";

	/**
	 * Will be resolved to the technical name of the module.
	 */
	public static final String XD_MODULE_NAME = "${" + XD_MODULE_NAME_KEY + "}";

	/**
	 * Will be resolved to the technical type of the module.
	 */
	public static final String XD_MODULE_TYPE = "${" + XD_MODULE_TYPE_KEY + "}";

	/**
	 * Will be resolved to the label of the module.
	 */
	public static final String XD_MODULE_LABEL = "${" + XD_MODULE_LABEL_KEY + "}";

	/**
	 * Will be resolved to the index (0-based) of the module inside the stream.
	 */
	public static final String XD_MODULE_INDEX = "${" + XD_MODULE_INDEX_KEY + "}";

}
