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

package org.springframework.xd.module.options.mixins;

import java.text.SimpleDateFormat;

import javax.validation.constraints.AssertFalse;

import org.hibernate.validator.constraints.NotBlank;

import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.validation.DateFormat;


/**
 * Provides a {@code dateFormat} module option, meant to hold a pattern that conforms to {@link SimpleDateFormat}.
 *
 * @author Eric Bottard
 */
public class DateFormatMixin {

	private String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	@NotBlank
	@DateFormat
	public String getDateFormat() {
		return dateFormat;
	}

	@ModuleOption("a pattern (as in SimpleDateFormat) for parsing/formatting dates and timestamps")
	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}


}
