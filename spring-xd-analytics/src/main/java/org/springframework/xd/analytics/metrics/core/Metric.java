package org.springframework.xd.analytics.metrics.core;

public interface Metric {

	String getName();
	
	long getValue();
}
