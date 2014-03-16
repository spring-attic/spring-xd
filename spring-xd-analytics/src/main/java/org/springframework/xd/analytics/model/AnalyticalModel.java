package org.springframework.xd.analytics.model;

import org.springframework.xd.tuple.Tuple;

/**
 * Author: Thomas Darimont
 */
public interface AnalyticalModel {

	String getId();

	Tuple evaluate(Tuple input);
}
