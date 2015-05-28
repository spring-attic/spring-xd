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

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.springframework.scheduling.support.CronSequenceGenerator;

/**
 * The annotated element must be a valid Spring cron expression (6 fields).
 *
 * @author Eric Bottard
 * @author Gary Russell
 */
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = { CronExpression.CronValidator.class })
public @interface CronExpression {

	String DEFAULT_MESSAGE = "";

	String message() default DEFAULT_MESSAGE;

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};


	/**
	 * Defines several {@link CronExpression} annotations on the same element.
	 *
	 * @see CronExpression
	 */
	@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
	@Retention(RUNTIME)
	@Documented
	@interface List {

		CronExpression[] value();
	}

	public static class CronValidator implements ConstraintValidator<CronExpression, CharSequence> {

		private String message;

		@Override
		public void initialize(CronExpression constraintAnnotation) {
			this.message = constraintAnnotation.message();
		}

		@Override
		public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
			// It's common behavior for validators to accept nulls (to be validated separately by NonNull)
			if (value == null) {
				return true;
			}
			try {
				new CronSequenceGenerator(value.toString());
			}
			catch (IllegalArgumentException e) {
				if (DEFAULT_MESSAGE.equals(this.message)) {
					context.disableDefaultConstraintViolation();
					context.buildConstraintViolationWithTemplate(e.getMessage()).addConstraintViolation();
				}
				return false;
			}
			return true;
		}
	}
}
