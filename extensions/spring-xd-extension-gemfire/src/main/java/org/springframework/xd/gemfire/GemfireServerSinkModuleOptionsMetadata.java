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


/**
 * Captures module options for the "gemfire-server" and "gemfire-json-server" sink modules.
 * 
 * @author Eric Bottard
 */
public class GemfireServerSinkModuleOptionsMetadata implements ProfileNamesProvider {

	private String host = "localhost";

	private int port = 40404;

	private String regionName;

	private String keyExpression;

	private boolean useLocator = false;


	public String getHost() {
		return host;
	}


	@ModuleOption("host name of the cache server or locator (if useLocator=true)")
	public void setHost(String host) {
		this.host = host;
	}


	public int getPort() {
		return port;
	}

	@ModuleOption("port of the cache server or locator (if useLocator=true)")
	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String[] profilesToActivate() {
		if (useLocator) {
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


	public boolean isUseLocator() {
		return useLocator;
	}

	@ModuleOption("indicates whether a locator is used to access the cache server")
	public void setUseLocator(boolean useLocator) {
		this.useLocator = useLocator;
	}

}
