package org.springframework.xd.analytics.model;

import org.springframework.xd.tuple.Tuple;

/**
 * Author: Thomas Darimont
 */
public interface AnalyticalModelEvaluator<M extends AnalyticalModel> {

	Tuple evaluate(Tuple input);
}
