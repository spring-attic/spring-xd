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

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.expression.ParseException;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;


/**
 * A JSR303 validator that validates that what is annotated with {@code @SpEL} can indeed be parsed as a SpEL
 * expression.
 * 
 * @author Eric Bottard
 */
public class SpELValidator implements ConstraintValidator<SpEL, CharSequence> {

	private boolean templated;

	private static final SpelExpressionParser parser = new SpelExpressionParser();

	private static final TemplateParserContext templateParserContext = new TemplateParserContext();

	@Override
	public void initialize(SpEL constraintAnnotation) {
		this.templated = constraintAnnotation.templated();
	}

	@Override
	public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		try {
			if (templated) {
				parser.parseExpression(value.toString(), templateParserContext);
			}
			else {
				parser.parseExpression(value.toString());
			}
			return true;
		}
		catch (ParseException _) {
			return false;
		}
	}

}
