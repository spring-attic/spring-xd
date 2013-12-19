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

package org.springframework.xd.dirt.server.options;

import javax.validation.constraints.NotNull;

import org.kohsuke.args4j.Option;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.xd.dirt.server.options.CommonDistributedOptions.Store;


/**
 * Holds options that can be used in single-node mode. Some of those are hardcoded to accept a single value on purpose
 * because.
 * 
 * @author Eric Bottard
 * @author David Turanski
 */
@ConfigurationProperties
public class SingleNodeOptions extends CommonOptions {

	public static enum Analytics {
		memory, redis;
	}

	public static enum ControlTransport {
		local, rabbit, redis;
	}

	@Option(name = "--analytics", usage = "How to persist analytics such as counters and gauges")
	private Analytics analytics;

	@Option(name = "--transport", usage = "The transport to use for data messages (from node to node)")
	private DataTransport transport;

	@Option(name = "--controlTransport", aliases = "--control-transport", usage = "The transport to use for control messages (between admin and nodes)")
	private ControlTransport controlTransport;

	@Option(name = "--store", usage = "How to persist admin data")
	private Store store;

	@Option(name = "--httpPort", usage = "Http port for the REST API server", metaVar = "<httpPort>")
	private Integer httpPort;

	@Option(name = "--hadoopDistro", usage = "The Hadoop distribution to be used for HDFS access")
	private HadoopDistro distro;

	public Integer getPORT() {
		return httpPort;
	}

	public void setPORT(int httpPort) {
		this.httpPort = httpPort;
	}

	@NotNull
	public Analytics getXD_ANALYTICS() {
		return analytics;
	}

	@NotNull
	public Store getXD_STORE() {
		return store;
	}

	@NotNull
	public DataTransport getXD_TRANSPORT() {
		return transport;
	}

	@NotNull
	public ControlTransport getXD_CONTROL_TRANSPORT() {
		return controlTransport;
	}

	public void setXD_ANALYTICS(Analytics analytics) {
		this.analytics = analytics;
	}

	public void setXD_STORE(Store store) {
		this.store = store;
	}

	public void setXD_TRANSPORT(DataTransport transport) {
		this.transport = transport;
	}

	public void setXD_CONTROL_TRANSPORT(ControlTransport controlTransport) {
		this.controlTransport = controlTransport;
	}

	public void setHADOOP_DISTRO(HadoopDistro distro) {
		this.distro = distro;
	}

	public HadoopDistro getHADOOP_DISTRO() {
		return this.distro;
	}
}
