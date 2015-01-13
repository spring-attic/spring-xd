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

import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;

import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_STREAM_NAME;


/**
 * Captures module options for the "gemfire-server" and "gemfire-json-server" sink modules.
 * 
 * @author Eric Bottard
 * @author David Turanski
 */
@Mixin(GemfireHostPortMixin.class)
public class GemfireServerSinkModuleOptionsMetadata  {

	private String regionName = XD_STREAM_NAME;

	private String keyExpression = "'" + XD_STREAM_NAME + "'";


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
