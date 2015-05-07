/*
 * Copyright 2015 the original author or authors.
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
 *
 *
 */

package org.springframework.xd.module.options.validation;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.util.Set;

import javax.validation.ConstraintViolation;

import org.junit.Before;
import org.junit.Test;

import org.springframework.validation.beanvalidation.CustomValidatorBean;

/**
 * Created by ebottard on 07/05/15.
 */
public class DateWithCustomFormatTests {

	CustomValidatorBean validator = new CustomValidatorBean();

	@Before
	public void setup() {
		validator.afterPropertiesSet();
	}

	@Test
	public void testViolation() {
		Bean bean = new Bean("25/12", "MM/dd/yyyy");
		ConstraintViolation<Bean> violation = validator.validate(bean).iterator().next();
		assertThat(violation.getMessage(),
				equalTo("Can not be parsed as a date using the format specified in 'theFormat', that is, 'MM/dd/yyyy'"));
		assertThat(violation.getPropertyPath().toString(), equalTo("date"));
	}

	@Test
	public void testNoViolation() {
		Bean bean = new Bean("25/12/2015", "dd/MM/yyyy");
		Set<ConstraintViolation<Bean>> violations = validator.validate(bean);
		assertThat(violations, is(empty()));
	}

	@Test
	public void testThatLenientDateValidationIsFalse() {
		Bean bean = new Bean("25/12/2015", "MM/dd/yyyy");
		ConstraintViolation<Bean> violation = validator.validate(bean).iterator().next();
		assertThat(violation.getMessage(),
				equalTo("Can not be parsed as a date using the format specified in 'theFormat', that is, 'MM/dd/yyyy'"));
		assertThat(violation.getPropertyPath().toString(), equalTo("date"));
	}

	@Test
	public void testNulls() {
		Bean bean = new Bean(null, null);
		Set<ConstraintViolation<Bean>> violations = validator.validate(bean);
		assertThat(violations, is(empty()));

		// No format
		bean = new Bean("25/12", null);
		violations = validator.validate(bean);
		assertThat(violations, is(empty()));

		// No date
		bean = new Bean(null, "MM/dd");
		violations = validator.validate(bean);
		assertThat(violations, is(empty()));
	}

	@Test
	public void testInvalidFormat() {
		Bean bean = new Bean("25/12", "foo");
		Set<ConstraintViolation<Bean>> violations = validator.validate(bean);
		assertThat(violations, is(empty()));

	}

	@DateWithCustomFormat(formatProperty = "theFormat")
	public static class Bean {

		private String date;

		private String theFormat;

		public Bean(String date, String theFormat) {
			this.date = date;
			this.theFormat = theFormat;
		}

		public String getDate() {
			return date;
		}

		public String getTheFormat() {
			return theFormat;
		}
	}
}
