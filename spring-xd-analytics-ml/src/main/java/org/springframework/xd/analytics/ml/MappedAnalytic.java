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

import org.springframework.util.Assert;

/**
 * @author Thomas Darimont
 */
public abstract class MappedAnalytic<I, O, MI, MO, A extends MappedAnalytic<I,O,MI,MO, A>> implements Analytic<I, O> {

	private final InputMapper<I, A, MI> inputMapper;
	private final OutputMapper<I, O, A, MO> outputMapper;

	/**
	 * Creates a new {@link MappedAnalytic}.
	 *
	 * @param inputMapper must not be {@literal null}.
	 * @param outputMapper must not be {@literal null}.
	 */
	public MappedAnalytic(InputMapper<I, A, MI> inputMapper, OutputMapper<I, O, A, MO> outputMapper) {

		Assert.notNull(inputMapper,"inputMapper");
		Assert.notNull(outputMapper,"outputMapper");

		this.inputMapper = inputMapper;
		this.outputMapper = outputMapper;
	}

	/**
	 *
	 * @param input
	 * @return
	 */
	@Override
	@SuppressWarnings("unchecked")
	public O evaluate(I input) {

		MI modelInput = inputMapper.mapInput((A) this, input);

		MO modelOutput = evaluateInternal(modelInput);

		O output = outputMapper.mapOutput((A) this, input, modelOutput);

		return output;
	}

	/**
	 *
	 * @param modelInput
	 * @return
	 */
	protected abstract MO evaluateInternal(MI modelInput);
}
