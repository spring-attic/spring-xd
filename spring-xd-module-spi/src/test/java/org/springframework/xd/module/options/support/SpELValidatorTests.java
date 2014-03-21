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

package org.springframework.xd.module.options.support;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.Set;

import javax.validation.ConstraintViolation;

import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Tests for {@link SpELValidator}.
 * 
 * @author Eric Bottard
 */
public class SpELValidatorTests {

	private static LocalValidatorFactoryBean validatorFactory;

	@BeforeClass
	public static void setUp() {
		validatorFactory = new LocalValidatorFactoryBean();
		validatorFactory.afterPropertiesSet();
	}

	@Test
	public void testInvalid() {
		Set<ConstraintViolation<SpELValidatorTests>> violations = validatorFactory.validateProperty(this, "invalid");
		assertThat(violations, not(empty()));
	}

	@Test
	public void testValid() {
		Set<ConstraintViolation<SpELValidatorTests>> violations = validatorFactory.validateProperty(this, "valid");
		assertThat(violations, empty());
	}

	@SpEL
	public String getInvalid() {
		return "foo + 3 +";
	}

	@SpEL
	public String getValid() {
		return "3 + 4";
	}

	@Test
	public void testInvalidWithTemplates() {
		Set<ConstraintViolation<SpELValidatorTests>> violations = validatorFactory.validateProperty(this,
				"invalidWithTemplates");
		assertThat(violations, not(empty()));
	}

	@Test
	public void testValidWithTemplates() {
		Set<ConstraintViolation<SpELValidatorTests>> violations = validatorFactory.validateProperty(this,
				"validWithTemplates");
		assertThat(violations, empty());
	}

	@SpEL(templated = true)
	public String getInvalidWithTemplates() {
		return "foo + 3 + #{5 / }";
	}

	@SpEL(templated = true)
	public String getValidWithTemplates() {
		return "3 + 4 + #{4 + 5}";
	}

}
