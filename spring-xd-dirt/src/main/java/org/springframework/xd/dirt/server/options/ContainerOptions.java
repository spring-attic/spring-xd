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
import org.springframework.xd.dirt.server.options.ResourcePatternScanningOptionHandlers.HadoopDistroOptionHandler;

/**
 * Holds configuration options that are valid for the Container node, when using distributed mode.
 *
 * @author Eric Bottard
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 */
@ConfigurationProperties
public class ContainerOptions extends CommonDistributedOptions {

	/* This is also used in SingleNodeOptions. */
	/*default*/static final String DEFAULT_HADOOP_DISTRO = "hadoop22";

	@Option(name = "--hadoopDistro", handler = HadoopDistroOptionHandler.class,
			usage = "The Hadoop distribution to be used for HDFS access")
	private String distro = DEFAULT_HADOOP_DISTRO;

	@Option(name = "--groups", usage = "The group memberships for this container as a comma delimited string")
	private String groups;

	@Option(name = "--containerHostname", usage = "The hostname of the container.")
	private String hostname;

	@Option(name = "--containerIp", usage = "The IP address of the container.")
	private String ip;

	public String getHADOOP_DISTRO() {
		return this.distro;
	}

	public String getXD_CONTAINER_HOSTNAME() {
		return hostname;
	}

	public String getXD_CONTAINER_IP() {
		return ip;
	}

	public String getXD_CONTAINER_GROUPS() {
		return this.groups;
	}


	public void setHADOOP_DISTRO(String distro) {
		this.distro = distro;
	}

	public void setXD_CONTAINER_HOSTNAME(String hostname) {
		this.hostname = hostname;
	}


	public void setXD_CONTAINER_IP(String ip) {
		this.ip = ip;
	}


	public void setXD_CONTAINER_GROUPS(String groups) {
		this.groups = groups;
	}

}
