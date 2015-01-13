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

package org.springframework.xd.gemfire;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Describes options to the {@code gemfire-cq} source module.
 * 
 * @author Eric Bottard
 * @author David Turanski
 */
@Mixin(GemfireHostPortMixin.class)
public class GemfireCQSourceOptionsMetadata{
	private String query;

	@NotBlank
	public String getQuery() {
		return query;
	}

	@ModuleOption("the query string in Object Query Language (OQL)")
	public void setQuery(String query) {
		this.query = query;
	}
}
