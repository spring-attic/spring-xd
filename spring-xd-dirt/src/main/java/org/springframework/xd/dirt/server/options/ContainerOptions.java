/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.server.options;

import org.kohsuke.args4j.Option;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Holds configuration options that are valid for the Container node, when using distributed mode.
 *
 * @author Eric Bottard
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 */
@ConfigurationProperties
public class ContainerOptions extends CommonDistributedOptions {

	@Option(name = "--hadoopDistro", usage = "The Hadoop distribution to be used for HDFS access")
	private HadoopDistro distro = DEFAULT_HADOOP_DISTRO;

	@Option(name = "--groups", usage = "The group memberships for this container as a comma delimited string")
	private String groups;

	public static final HadoopDistro DEFAULT_HADOOP_DISTRO = HadoopDistro.hadoop22;

	public void setHADOOP_DISTRO(HadoopDistro distro) {
		this.distro = distro;
	}

	public HadoopDistro getHADOOP_DISTRO() {
		return this.distro;
	}

	public void setXD_CONTAINER_GROUPS(String groups) {
		this.groups = groups;
	}

	public String getXD_CONTAINER_GROUPS() {
		return this.groups;
	}
}
