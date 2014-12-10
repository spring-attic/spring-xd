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

package org.springframework.xd.spark.streaming.examples;

import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.spark.streaming.DefaultSparkStreamingModuleOptionsMetadata;

/**
 * @author Ilayaperumal Gopinathan
 */
public class SparkLogModuleOptionsMetadata extends DefaultSparkStreamingModuleOptionsMetadata {

	private String filePath;

	@ModuleOption("the file path for the log module")
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getFilePath() {
		return this.filePath;
	}
}
