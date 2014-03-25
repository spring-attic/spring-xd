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

import org.springframework.xd.tuple.Tuple;

/**
 * Processes {@link Tuple}s by applying a configured {@link Analytic}.
 *
 * @author Thomas Darimont
 */
public class AnalyticProcessor{

    /**
     * Holds the {@link org.springframework.xd.analytics.ml.Analytic} to use in the {@code #process} method.
     */
	private Analytic<Tuple, Tuple> analytic; //TODO add support for thread-safe hot swap of new analytics.

	/**
	 * Processes the given {@link org.springframework.xd.tuple.Tuple} by applying the configured {@link org.springframework.xd.analytics.ml.Analytic}.
	 * Potentially copies the {@code input} and applies the configured {@code analytic}.
     *
	 * @param input
	 * @return a new {@link org.springframework.xd.tuple.Tuple} if {@code analytic} was applied or the original {@code}.
	 */
	public Tuple process(Tuple input){

		if(this.analytic == null){
			return input;
		}

		return this.analytic.evaluate(input);
	}

	public Analytic<Tuple, Tuple> getAnalytic() {
		return analytic;
	}

	public void setAnalytic(Analytic<Tuple, Tuple> analytic) {
		this.analytic = analytic;
	}
}
