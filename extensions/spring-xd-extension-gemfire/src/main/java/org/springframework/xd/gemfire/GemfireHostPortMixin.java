/*
 * Copyright 2015 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.gemfire;

import org.springframework.util.StringUtils;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;

import javax.validation.constraints.AssertTrue;

/**
 * @author David Turanski
 */
public class GemfireHostPortMixin implements ProfileNamesProvider {
	private String host;

	private String port;

	private boolean useLocator;

	public String getHost() {
		return host;
	}

	@ModuleOption("host name of the cache server or locator (if useLocator=true). May be a comma delimited list")
	public void setHost(String host) {
		this.host = host;
	}

	public String getPort() {
		return port;
	}

	@ModuleOption("port of the cache server or locator (if useLocator=true). May be a comma delimited list")
	public void setPort(String port) {
		this.port = port;
	}

	public boolean isUseLocator() {
		return useLocator;
	}

	@ModuleOption("indicates whether a locator is used to access the cache server")
	public void setUseLocator(boolean useLocator) {
		this.useLocator = useLocator;
	}

	@Override
	public String[] profilesToActivate() {
		if (useLocator) {
			return new String[] {"use-locator"};
		}
		else {
			return new String[] {"use-server"};
		}
	}

	@AssertTrue(message = "host and port may be comma delimited strings or single values - either one-to-many or the " +
			"same length")
	boolean isHostAndPortListsValid() {
		String[] hosts = host.split(",");
		String[] ports = port.split(",");
		for (String h: hosts) {
			if (!StringUtils.hasText(h)) {
				return false;
			}
		}

		for (String p: ports) {
			if (!StringUtils.hasText(p)) {
				return false;
			}
		}

		if (hosts.length != ports.length) {
			return hosts.length == 1 || ports.length == 1;
		}
		return true;
	}

	@AssertTrue(message = "port must be a valid integer value (0 - 65535)")
	boolean isPortValidValue() {
		String[] ports = port.split(",");
		for (String p : ports) {
			try {
				int val = Integer.parseInt(p.trim());
				if (val < 0 || val > 65535) {
					return false;
				}
			}
			catch (Exception e) {
				return false;
			}
		}
		return true;
	}
}
