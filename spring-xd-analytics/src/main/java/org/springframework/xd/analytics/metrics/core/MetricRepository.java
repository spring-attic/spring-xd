package org.springframework.xd.analytics.metrics.core;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;

/**
 * A repository to save, delete and find Metric instances.  Uses the Spring Data Repository
 * marker interface and conventions for method names and behavior.
 *
 * The name is the id and should be unique.
 *
 * Note: Will shift to inherit from CrudRepository
 *
 * @author Mark Pollack
 * @author Luke Taylor
 */
public interface MetricRepository<M extends Metric> extends CrudRepository<M, String> {

}
