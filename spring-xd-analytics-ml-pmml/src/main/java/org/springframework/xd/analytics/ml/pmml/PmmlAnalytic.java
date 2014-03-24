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
package org.springframework.xd.analytics.ml.pmml;

import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.manager.PMMLManager;
import org.springframework.util.Assert;
import org.springframework.xd.analytics.ml.InputMapper;
import org.springframework.xd.analytics.ml.MappedAnalytic;
import org.springframework.xd.analytics.ml.OutputMapper;

/**
 * A {@link org.springframework.xd.analytics.ml.MappedAnalytic} that can evaluate {@link org.dmg.pmml.PMML} models.
 *
 * @author Thomas Darimont
 */
public class PmmlAnalytic<I, O> extends MappedAnalytic<I, O, Map<FieldName, Object>, Map<FieldName, Object>, PmmlAnalytic<I, O>> {

	private final Log log = LogFactory.getLog(this.getClass());

	private final String name;

	private final PMML pmml;

	private final PMMLManager pmmlManager;

	private final Evaluator pmmlEvaluator;

	/**
	 * Creates a new {@link PmmlAnalytic}.
	 *
	 * @param name must not be {@literal null}.
	 * @param pmmlResolver must not be {@literal null}.
	 * @param inputMapper must not be {@literal null}.
	 * @param outputMapper must not be {@literal null}.
	 */
	public PmmlAnalytic(String name,
						PmmlResolver pmmlResolver,
						InputMapper<I, PmmlAnalytic<I, O>, Map<FieldName, Object>> inputMapper,
						OutputMapper<I, O, PmmlAnalytic<I, O>, Map<FieldName, Object>> outputMapper) {

		super(inputMapper, outputMapper);

		Assert.notNull(name, "name");
		Assert.notNull(pmmlResolver, "pmmlResolver");

		this.name = name;
		this.pmml = pmmlResolver.getPmml(name, generateId());
		this.pmmlManager = new PMMLManager(pmml);
		this.pmmlEvaluator = (Evaluator) this.pmmlManager.getModelManager(null, ModelEvaluatorFactory.getInstance());

		if (log.isDebugEnabled()) {
			log.debug("PmmlAnalytic created");
		}
	}

	/**
	 * @return
	 */
	protected String generateId() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Evaluates the given {@code modelInput} with the analytic provided by {@link PMML} definition.
	 *
	 * @param modelInput
	 * @return
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected Map<FieldName, Object> evaluateInternal(Map<FieldName, Object> modelInput) {

		if (log.isDebugEnabled()) {
			log.debug("Before pmml evaluation - input: " + modelInput);
		}

		Map<FieldName, Object> result = (Map<FieldName, Object>) this.pmmlEvaluator.evaluate(modelInput);

		if (log.isDebugEnabled()) {
			log.debug("After pmml evaluation - result: " + result);
		}

		return result;
	}

	/**
	 * Returns the default {@link org.dmg.pmml.Model} of the wrapped {@link PMML} object.
	 * According to the PMML specification, this is the first {@code Model} in the {@code PMML} structure.
	 *
	 * @return
	 */
	Model getDefaultModel() {
		return this.pmml.getModels().get(0);
	}

	@Override
	public String toString() {
		return "PmmlAnalytic{" + "name='" + name + '\'' + '}' + "@" + Integer.toHexString(System.identityHashCode(this));
	}
}
