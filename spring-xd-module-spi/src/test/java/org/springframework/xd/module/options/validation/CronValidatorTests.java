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

import javax.validation.ConstraintViolation;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;

import java.util.Set;

import org.springframework.validation.beanvalidation.CustomValidatorBean;

/**
 * Tests for CronExpression.CronValidator.
 *
 * @author Eric Bottard
 */
public class CronValidatorTests {

	CustomValidatorBean validator = new CustomValidatorBean();

	@Before
	public void setup() {
		validator.afterPropertiesSet();
	}

	@Test
	public void testDefaultMessage() {
		Bean bean = new Bean();
		bean.cron1 = "* *";
		ConstraintViolation<Bean> violation = validator.validate(bean).iterator().next();
		Assert.assertThat(violation.getMessage(), equalTo("Cron expression must consist of 6 fields (found 2 in \"* *\")"));
	}

	@Test
	public void testExplicitMessage() {
		Bean bean = new Bean();
		bean.cron2 = "61 * * * * *";
		ConstraintViolation<Bean> violation = validator.validate(bean).iterator().next();
		Assert.assertThat(violation.getMessage(), equalTo("something is wrong"));

	}

	@Test
	public void testValidExpression() {
		Bean bean = new Bean();
		bean.cron2 = "* * * * * *";
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

		@CronExpression
		private String cron1;

		@CronExpression(message = "something is wrong")
		private String cron2;

	}

}
