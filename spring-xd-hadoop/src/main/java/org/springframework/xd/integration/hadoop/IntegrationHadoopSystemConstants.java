/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.integration.hadoop;

/**
 * Various constants throughout the spring integration hadoop libraries.
 * 
 * @author Janne Valkealahti
 */
public abstract class IntegrationHadoopSystemConstants {

	/** Default bean id for store writer. */
	public static final String DEFAULT_ID_STORE_WRITER = "storeWriter";

	/** Default bean id for file naming policy. */
	public static final String DEFAULT_ID_FILE_NAMING_STRATEGY = "fileNamingStrategy";

	/** Default bean id for file rollover policy. */
	public static final String DEFAULT_ID_FILE_ROLLOVER_STRATEGY = "fileRolloverStrategy";

	/** Default writer path. */
	public static final String DEFAULT_DATA_PATH = "/xd";

}
