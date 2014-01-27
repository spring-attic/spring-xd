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
 * Captures module options for the "jdbc" sink module.
 * 
 * @author Eric Bottard
 */
public class JdbcSinkModuleOptionsMetadata extends AbstractJdbcModuleOptionsMetadata {

	private String columns;


	public JdbcSinkModuleOptionsMetadata() {
		columns = "payload";
		configProperties = "jdbc";
	}

	@ModuleOption("the database columns to map the data to")
	public void setColumns(String columns) {
		this.columns = columns;
	}

	public String getColumns() {
		return columns;
	}

}
