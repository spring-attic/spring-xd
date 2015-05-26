/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.modules.metadata;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Module options for header enricher processor module.
 * 
 * @author Franck Marchand
 */
public class HeaderEnricherProcessorOptionsMetadata {

	private String expression;

	private String name;

	private boolean overwrite = true;

	public boolean isOverwrite() {
		return overwrite;
	}

	@ModuleOption("overwrite an existing header")
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	@NotBlank
	public String getExpression() {
		return expression;
	}

	@ModuleOption("a SpEL expression which would typically add a header")
	public void setExpression(String expression) {
		this.expression = expression;
	}

	@NotBlank
	public String getName() {
		return name;
	}

	@ModuleOption("name of the header to add")
	public void setName(String name) {
		this.name = name;
	}
}
