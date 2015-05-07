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

package org.springframework.xd.module.options.validation;

import static org.hamcrest.CoreMatchers.equalTo;

import java.util.Set;

import javax.validation.ConstraintViolation;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.validation.beanvalidation.CustomValidatorBean;

/**
 * Tests for DateFormat.DateFormatValidator.
 *
 * @author Eric Bottard
 */
public class DateFormatValidatorTests {
	CustomValidatorBean validator = new CustomValidatorBean();

	@Before
	public void setup() {
		validator.afterPropertiesSet();
	}

	@Test
	public void testDefaultMessage() {
		Bean bean = new Bean();
		bean.format1 = "xx";
		ConstraintViolation<Bean> violation = validator.validate(bean).iterator().next();
		Assert.assertThat(violation.getMessage(), equalTo("Illegal pattern character 'x'"));
	}

	@Test
	public void testExplicitMessage() {
		Bean bean = new Bean();
		bean.format2 = "xx";
		ConstraintViolation<Bean> violation = validator.validate(bean).iterator().next();
		Assert.assertThat(violation.getMessage(), equalTo("not a valid date format"));

	}

	@Test
	public void testValidExpression() {
		Bean bean = new Bean();
		bean.format2 = "MM/dd";
		Set<ConstraintViolation<Bean>> violations = validator.validate(bean);
		Assert.assertThat(violations, Matchers.empty());
	}

	@Test
	public void testNullIsOk() {
		Bean bean = new Bean();
		Set<ConstraintViolation<Bean>> violations = validator.validate(bean);
		Assert.assertThat(violations, Matchers.empty());
	}


	public static class Bean {

		@DateFormat
		private String format1;

		@DateFormat(message = "not a valid date format")
		private String format2;
	}
}
