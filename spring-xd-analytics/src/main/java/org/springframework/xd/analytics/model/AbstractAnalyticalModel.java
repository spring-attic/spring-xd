package org.springframework.xd.analytics.model;

/**
 * Author: Thomas Darimont
 */
public abstract class AbstractAnalyticalModel implements AnalyticalModel{

	private String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
