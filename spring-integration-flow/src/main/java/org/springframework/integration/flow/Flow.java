package org.springframework.integration.flow;

import org.springframework.integration.module.IntegrationModule;

/**
 * 
 * @author David Turanski
 * @since 3.0
 * 
 */
public class Flow extends IntegrationModule  {
	protected final static String MODULE_TYPE = "flow";
	/**
	 * @param name
	 */
	public Flow(String name) {
		super(name, MODULE_TYPE);
	}

}
