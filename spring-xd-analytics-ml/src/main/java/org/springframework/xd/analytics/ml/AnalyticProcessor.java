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
import org.springframework.xd.tuple.TupleBuilder;

/**
 * @author Thomas Darimont
 */
public class AnalyticProcessor{

	private Analytic<Tuple, Tuple> analytic;

	/**
	 *
	 * @param payload
	 * @return
	 */
	public Tuple transform(String payload){

		Tuple input = TupleBuilder.fromString(payload);

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
