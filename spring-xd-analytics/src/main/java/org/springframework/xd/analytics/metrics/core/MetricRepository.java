package org.springframework.xd.analytics.metrics.core;

import org.springframework.data.repository.CrudRepository;

/**
 * A repository to save, delete and find Metric instances.  Uses the Spring Data CrudRepository
 * marker interface and conventions for method names and behavior.
 *
 * The name is the id and should be unique.
 *
 * @author Mark Pollack
 * @author Luke Taylor
 */
public interface MetricRepository<M extends Metric> extends CrudRepository<M, String> {

}
