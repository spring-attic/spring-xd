package org.springframework.xd.analytics.model;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/**
 * Author: Thomas Darimont
 */
@NoRepositoryBean
public interface AnalyticalModelRepository<M extends AnalyticalModel> extends Repository {

	M getById(String id);

}
