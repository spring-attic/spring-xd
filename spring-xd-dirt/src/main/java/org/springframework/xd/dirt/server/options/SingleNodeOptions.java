/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.server.options;


/**
 * A class the defines the options that will be parsed on the container command line.
 */
public class SingleNodeOptions extends AdminOptions {

	public SingleNodeOptions() {
		super(Transport.local, Analytics.memory);
		this.store = Store.memory;
	}

	public ContainerOptions asContainerOptions() {
		ContainerOptions containerOptions = new ContainerOptions();
		containerOptions.setAnalytics(this.getAnalytics());
		containerOptions.setHadoopDistro(this.getHadoopDistro());
		containerOptions.setJmxEnabled(this.isJmxEnabled());
		containerOptions.jmxPort = this.jmxPort;
		containerOptions.setShowHelp(this.isShowHelp());
		containerOptions.setTransport(this.getTransport());
		containerOptions.setXdHomeDir(this.getXdHomeDir());
		// Clear explicit options for the container in single node case. As configuration
		// has already happened
		for (Object key : getOptionMetadataCache().keySet()) {
			getOptionMetadataCache().put(key, false);
		}
		containerOptions.getOptionMetadataCache().putAll(this.getOptionMetadataCache());
		return containerOptions;
	}


}
