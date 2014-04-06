/*
 * Copyright 2014 the original author or authors.
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
 */

package org.springframework.xd.analytics.ml;

/**
 * An {@code InputMapper} maps a given domain input {@code I} to an appropriate model input {@code MI} for the given
 * analytic {@code A}.
 *
 * @author Thomas Darimont
 * @param <I> the input type
 * @param <A> the analytic model type
 * @param <MI> the model input type
 */
public interface InputMapper<I, A, MI> extends Mapper{

	/**
	 * Maps the given input {@code I} into an appropriate model-input {@code MI} for the given
	 * {@link org.springframework.xd.analytics.ml.Analytic} {@code A}.
	 * 
	 * @param analytic
	 * @param input
	 * @return the input for this {@code Analytic}
	 */
	MI mapInput(A analytic, I input);
}
