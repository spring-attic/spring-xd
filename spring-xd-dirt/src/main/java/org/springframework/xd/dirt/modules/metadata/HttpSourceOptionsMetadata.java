/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.xd.dirt.modules.metadata;

import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Describes options to the {@code http} source module.
 *
 * @author Gary Russell
 */
public class HttpSourceOptionsMetadata {

	private int port = 9000;

	private String path = "/";

	private boolean https;

	private String sslPropertiesLocation = "classpath:httpSSL.properties";

	private int maxContentLength = 1048576;

	private String messageConverterClass = "org.springframework.integration.x.http.NettyInboundMessageConverter";

	public int getPort() {
		return port;
	}

	@ModuleOption("the port to listen to")
	public void setPort(int port) {
		this.port = port;
	}

	public String getPath() {
		return path;
	}

	@ModuleOption("the path of this source")
	public void setPath(String path) {
		this.path = path;
	}

	public boolean isHttps() {
		return https;
	}

	@ModuleOption("true for https://")
	public void setHttps(boolean https) {
		this.https = https;
	}

	public String getSslPropertiesLocation() {
		return sslPropertiesLocation;
	}

	@ModuleOption("location (resource) of properties containing the location of the pkcs12 keyStore and pass phrase")
	public void setSslPropertiesLocation(String sslProperties) {
		this.sslPropertiesLocation = sslProperties;
	}

	public int getMaxContentLength() {
		return maxContentLength;
	}

	@ModuleOption("the maximum allowed content length")
	public void setMaxContentLength(int maxContentLength) {
		this.maxContentLength = maxContentLength;
	}

	public String getMessageConverterClass() {
		return messageConverterClass;
	}

	@ModuleOption("the name of a custom MessageConverter class, to convert HttpRequest to Message; must have a constructor with a 'MessageBuilderFactory' parameter")
	public void setMessageConverterClass(String messageConverterClass) {
		this.messageConverterClass = messageConverterClass;
	}

}
