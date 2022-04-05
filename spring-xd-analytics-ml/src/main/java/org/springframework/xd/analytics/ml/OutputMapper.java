/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.analytics.ml;

/**
 * An {@code OutputMapper} maps a given model input {@code MO} to an appropriate domain input {@code O} for the given
 * analytic {@code A} by potentially using data from the original input {@code I}.
 *
 * @author Thomas Darimont
 * @param <I> the input type
 * @param <O> the output type
 * @param <A> the analytic model type
 * @param <MO> the model output type
 */
public interface OutputMapper<I, O, A, MO> extends Mapper {

	/**
	 * Maps the model-output {@code MO} to an appropriate output {@code O}.
	 * 
	 * @param analytic the {@link org.springframework.xd.analytics.ml.Analytic} that can be used to retrieve mapping
	 *        information.
	 * @param input the input for this {@link org.springframework.xd.analytics.ml.Analytic} that could be used to build
	 *        the model {@code O}.
	 * @param modelOutput the raw unmapped model output {@code MO}.
	 * @return the actual output of the {@code Analytic} {@code A}.
	 */
	O mapOutput(A analytic, I input, MO modelOutput);
}
