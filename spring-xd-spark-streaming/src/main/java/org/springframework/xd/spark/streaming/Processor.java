/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.spark.streaming;

import java.io.Serializable;

import org.apache.spark.streaming.api.java.JavaDStreamLike;

/**
 * Interface for modules using the Spark Streaming API to process messages from the Message Bus.
 *
 * @author Mark Fisher
 */
@SuppressWarnings("rawtypes")
public interface Processor extends Serializable {

	public static final String SPARK_MASTER_URL_PROP = "spark.master";

	public static final String SPARK_STORAGE_LEVEL_PROP = "spark.storageLevel";

	public static final String SPARK_STORAGE_LEVEL_MODULE_OPTION = "storageLevel";

	/**
	 * The module execution framework is used by XD runtime to determine the module as
	 * the spark streaming module.
	 */
	public static final String MODULE_EXECUTION_FRAMEWORK = "spark";

	public static final String SPARK_DEFAULT_MASTER_URL = "spark://localhost:7077";

	/**
	 * Processes the input DStream and optionally returns an output DStream.
	 *
	 * @param input the input DStream
	 * @return output DStream (optional, may be null)
	 */
	JavaDStreamLike process(JavaDStreamLike input);
}
