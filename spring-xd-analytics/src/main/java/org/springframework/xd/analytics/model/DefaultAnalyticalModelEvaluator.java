package org.springframework.xd.analytics.model;

import org.springframework.xd.tuple.Tuple;

/**
 * Author: Thomas Darimont
 */
public class DefaultAnalyticalModelEvaluator<M extends AnalyticalModel> implements AnalyticalModelEvaluator<M>{

	private M model;

	@Override
	public Tuple evaluate(Tuple input) {
		return getModel().evaluate(input);
	}

	public M getModel() {
		return model;
	}

	public void setModel(M model) {
		this.model = model;
	}
}
