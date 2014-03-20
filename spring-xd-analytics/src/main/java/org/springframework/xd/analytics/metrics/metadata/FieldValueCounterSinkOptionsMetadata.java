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

package org.springframework.xd.analytics.metrics.metadata;

import org.hibernate.validator.constraints.NotBlank;

import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Captures options for the {@code field-value-counter} sink module.
 * 
 * @author Eric Bottard
 */
@Mixin({ MetricNameMixin.class })
public class FieldValueCounterSinkOptionsMetadata {

	private String fieldName = null;

	@NotBlank
	public String getFieldName() {
		return fieldName;
	}

	@ModuleOption("the name of the field for which values are counted")
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}


}
