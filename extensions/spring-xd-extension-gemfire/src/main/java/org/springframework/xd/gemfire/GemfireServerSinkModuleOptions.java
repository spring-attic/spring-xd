/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.gemfire;

import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;
import org.springframework.xd.module.options.spi.SinkModuleOptionsMetadataSupport;


/**
 * Captures module options for the "gemfire-server" and "gemfire-json-server" sink modules.
 * 
 * @author Eric Bottard
 */
public class GemfireServerSinkModuleOptions extends SinkModuleOptionsMetadataSupport implements ProfileNamesProvider {

	private String gemfireHost = "localhost";

	private int gemfirePort = 40404;

	private String locatorHost;

	private Integer locatorPort;

	private String regionName;

	private String keyExpression = "payload";


	public String getGemfireHost() {
		return gemfireHost;
	}


	@ModuleOption("hostname of a single gemfire host to target")
	public void setGemfireHost(String gemfireHost) {
		this.gemfireHost = gemfireHost;
	}


	public int getGemfirePort() {
		return gemfirePort;
	}

	@ModuleOption("port of a single gemfire host to target")
	public void setGemfirePort(int gemfirePort) {
		this.gemfirePort = gemfirePort;
	}


	// public String getLocatorHost() {
	// return locatorHost;
	// }
	//
	// @ModuleOption("hostname to target when using a gemfire locator")
	// public void setLocatorHost(String locatorHost) {
	// this.locatorHost = locatorHost;
	// }


	// Use wrapper class so that no default is reported
	// public Integer getLocatorPort() {
	// return locatorPort;
	// }

	// @ModuleOption("port to target when using a gemfire locator")
	// Use primitive type, so that null is not allowed
	// public void setLocatorPort(int locatorPort) {
	// this.locatorPort = locatorPort;
	// }


	@Override
	public String[] profilesToActivate() {
		if (locatorHost != null) {
			return new String[] { "use-locator" };
		}
		else {
			return new String[] { "use-server" };
		}
	}


	public String getRegionName() {
		return regionName;
	}


	@ModuleOption("name of the region to use when storing data")
	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}


	public String getKeyExpression() {
		return keyExpression;
	}


	@ModuleOption("a SpEL expression which is evaluated to create a cache key")
	public void setKeyExpression(String keyExpression) {
		this.keyExpression = keyExpression;
	}


}
