
package org.springframework.xd.analytics.metrics.core;

/**
 * Super interface for all metrics. Metrics all have a name which must be unique (per
 * kind).
 * 
 * @author Luke Taylor
 */
public interface Metric {

	/**
	 * Return the name of the metric.
	 */
	String getName();
}
