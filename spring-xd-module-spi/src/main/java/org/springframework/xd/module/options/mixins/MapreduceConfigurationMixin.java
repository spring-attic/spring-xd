/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.module.options.mixins;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Factors out common configuration options for Hadoop MapReduce.
 * 
 * @author Thomas Risberg
 */
public class MapreduceConfigurationMixin {

	private String resourceManagerHost = "${spring.hadoop.resourceManagerHost}";
	private String resourceManagerPort = "${spring.hadoop.resourceManagerPort}";


	@NotBlank
	public String getResourceManagerHost() {
		return resourceManagerHost;
	}

	@ModuleOption("the Host for Hadoop's ResourceManager")
	public void setResourceManagerHost(String resourceManagerHost) {
		this.resourceManagerHost = resourceManagerHost;
	}

	@NotBlank
	public String getResourceManagerPort() {
		return resourceManagerPort;
	}

	@ModuleOption("the Port for Hadoop's ResourceManager")
	public void setResourceManagerPort(String resourceManagerPort) {
		this.resourceManagerPort = resourceManagerPort;
	}
}
