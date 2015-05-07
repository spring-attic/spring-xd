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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/**
 * Used as a class level annotation to cross-validate a String representation of a date according to a date format
 * which is itself a property of the annotated class.
 *
 * @author Eric Bottard
 */
@Target({ TYPE })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = { DateWithCustomFormat.DateWithCustomFormatValidator.class })
public @interface DateWithCustomFormat {

	String DEFAULT_MESSAGE = "";

	String message() default DEFAULT_MESSAGE;

	/**
	 * The name of the property in the annotated class which holds a String representation of a Date.
	 */
	String dateProperty() default "date";

	/**
	 * The name of the property in the annotated class which holds the date pattern.
	 * <p>That property is encouraged to bear the @{@link DateFormat} constraint.</p>
	 */
	String formatProperty() default "dateFormat";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};


	/**
	 * Defines several {@link DateWithCustomFormat} annotations on the same element.
	 *
	 * @see DateWithCustomFormat
	 */
	@Target({ TYPE })
	@Retention(RUNTIME)
	@Documented
	@interface List {

		DateWithCustomFormat[] value();
	}


	public static class DateWithCustomFormatValidator implements ConstraintValidator<DateWithCustomFormat, Object> {

		private String dateProperty;

		private String formatProperty;

		private String message;

		@Override
		public void initialize(DateWithCustomFormat constraintAnnotation) {
			this.dateProperty = constraintAnnotation.dateProperty();
			this.formatProperty = constraintAnnotation.formatProperty();
			this.message = constraintAnnotation.message();
		}

		@Override
		public boolean isValid(Object value, ConstraintValidatorContext context) {
			BeanWrapper beanWrapper = new BeanWrapperImpl(value);
			String date = (String) beanWrapper.getPropertyValue(dateProperty);
			String format = (String) beanWrapper.getPropertyValue(formatProperty);
			if (date == null || format == null) {
				return true;
			}
			SimpleDateFormat sdf = null;
			try {
				sdf = new SimpleDateFormat(format);
				sdf.setLenient(false);
			}
			catch (IllegalArgumentException e) {
				// this is ignored, as an illegal format should be reported by @DateFormat
				return true;
			}

			try {
				sdf.parse(date);
			}
			catch (ParseException e) {

				if (DEFAULT_MESSAGE.equals(this.message)) {
					context.disableDefaultConstraintViolation();
					String message = String.format(
							"Can not be parsed as a date using the format specified in '%s', that is, '%s'",
							formatProperty, format);
					context.buildConstraintViolationWithTemplate(message).addPropertyNode(dateProperty).addConstraintViolation();
				}
				return false;
			}
			return true;
		}
	}

}
