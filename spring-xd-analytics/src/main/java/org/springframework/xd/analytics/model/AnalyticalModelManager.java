package org.springframework.xd.analytics.model;

import java.util.concurrent.Future;

/**
 * Author: Thomas Darimont
 */
public interface AnalyticalModelManager<M extends AnalyticalModel>{

	M getModel(String id);

	Future<M> requestModelUpdate(String id);
}
