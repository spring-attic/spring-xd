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

package org.springframework.xd.module.info;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Holder object for various bits of information about a module, such as description, <i>etc.</i>
 *
 * @author Eric Bottard
 */
public class ModuleInformation {

	@Size(max = 130)
	@Pattern(regexp = "^\\p{IsUppercase}.*\\.$", message = "Short description must start with a capital letter and end with a dot")
	private String shortDescription;

	public String getShortDescription() {
		return shortDescription;
	}

	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}
}
