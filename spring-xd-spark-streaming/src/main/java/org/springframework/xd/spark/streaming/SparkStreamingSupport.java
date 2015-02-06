/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.xd.spark.streaming;

import java.io.Serializable;

/**
 * Base interface for spring XD and spark streaming integration.
 *
 * @author Ilayaperumal Gopinathan
 * @since 1.1
 */
public interface SparkStreamingSupport extends Serializable {

	public static final String SPARK_MASTER_URL_PROP = "spark.master";

	public static final String SPARK_DEFAULT_MASTER_URL = "spark://localhost:7077";

	public static final String SPARK_STORAGE_LEVEL_PROP = "spark.storageLevel";

	public static final String SPARK_STORAGE_LEVEL_MODULE_OPTION = "storageLevel";

	public static final String SPARK_DEFAULT_STORAGE_LEVEL = "MEMORY_ONLY";

	public static final String SPARK_STREAMING_BATCH_INTERVAL_PROP = "spark.streaming.batchInterval";

	public static final String SPARK_STREAMING_BATCH_INTERVAL_MODULE_OPTION = "batchInterval";

	public static final String SPARK_STREAMING_DEFAULT_BATCH_INTERVAL = "2000";

	/**
	 * The module execution framework is used by XD runtime to determine the module as
	 * the spark streaming module.
	 */
	public static final String MODULE_EXECUTION_FRAMEWORK = "spark";

}
