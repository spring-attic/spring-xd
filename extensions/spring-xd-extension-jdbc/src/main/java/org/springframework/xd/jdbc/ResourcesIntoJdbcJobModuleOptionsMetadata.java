/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.jdbc;

import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Typical class for metadata about jobs that slurp csv resources into jdbc. Can be used as is or extended if needed.
 * 
 * @author Eric Bottard
 */
public class ResourcesIntoJdbcJobModuleOptionsMetadata extends
		AbstractJdbcModuleOptionsMetadata {

	private Boolean restartable;

	private String names;

	private String resources;


	public ResourcesIntoJdbcJobModuleOptionsMetadata() {
		configProperties = "batch-jdbc";
	}

	@ModuleOption("whether the job should be restartable or not in case of failure")
	public void setRestartable(Boolean restartable) {
		this.restartable = restartable;
	}

	@ModuleOption("the field names in the CSV file, used to map the data to the corresponding table columns")
	public void setNames(String names) {
		this.names = names;
	}

	@ModuleOption("the list of paths to import (Spring resources)")
	public void setResources(String resources) {
		this.resources = resources;
	}

	public Boolean getRestartable() {
		return restartable;
	}

	public String getNames() {
		return names;
	}

	public String getResources() {
		return resources;
	}

}
