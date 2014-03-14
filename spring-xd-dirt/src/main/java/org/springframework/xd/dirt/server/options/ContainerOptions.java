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

import java.util.HashMap;
import java.util.Map;

import org.kohsuke.args4j.Option;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.xd.dirt.server.options.ResourcePatternScanningOptionHandlers.DistributedDataTransportOptionHandler;

/**
 * Holds configuration options that are valid for the Container node, when using distributed mode.
 * 
 * @author Eric Bottard
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 */
@ConfigurationProperties
public class ContainerOptions extends CommonDistributedOptions {

	@Option(name = "--transport", handler = DistributedDataTransportOptionHandler.class,
			usage = "The transport to use for data messages (from node to node)")
	private String transport;

	@Option(name = "--hadoopDistro", usage = "The Hadoop distribution to be used for HDFS access")
	private HadoopDistro distro = DEFAULT_HADOOP_DISTRO;

	public static final HadoopDistro DEFAULT_HADOOP_DISTRO = HadoopDistro.hadoop22;

	private static final Map<HadoopDistro, String> hadoopDistroVersions = new HashMap<HadoopDistro, String>();

	public void setXD_TRANSPORT(String transport) {
		this.transport = transport;
	}

	public String getXD_TRANSPORT() {
		return this.transport;
	}

	public void setHADOOP_DISTRO(HadoopDistro distro) {
		this.distro = distro;
	}

	public HadoopDistro getHADOOP_DISTRO() {
		return this.distro;
	}

	public static Map<HadoopDistro, String> getHadoopDistroVersions() {
		hadoopDistroVersions.put(HadoopDistro.hadoop12, "1.2");
		hadoopDistroVersions.put(HadoopDistro.hdp13, "1.3");
		hadoopDistroVersions.put(HadoopDistro.hdp20, "2.0");
		hadoopDistroVersions.put(HadoopDistro.hadoop22, "2.2");
		hadoopDistroVersions.put(HadoopDistro.cdh4, "cdh4");
		hadoopDistroVersions.put(HadoopDistro.phd1, "gphd");
		return hadoopDistroVersions;
	}
}
