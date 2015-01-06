/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.xd.dirt.modules.metadata;

import org.hibernate.validator.constraints.NotBlank;

import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;

/**
 * Factors out common options about configuring a connection to an MQTT broker.
 * 
 * @author Eric Bottard
 * @author Gary Russell
 */
public class MqttConnectionMixin implements ProfileNamesProvider {

	private String url = "tcp://localhost:1883";

	private String username = "guest";

	private String password = "guest";

	private boolean cleanSession = true;

	private int connectionTimeout = 30;

	private int keepAliveInterval = 60;

	private String persistence = "memory";

	private String persistenceDirectory = "/tmp/paho";

	@NotBlank
	public String getUrl() {
		return url;
	}

	@ModuleOption("location of the mqtt broker(s) (comma-delimited list)")
	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	@ModuleOption("the username to use when connecting to the broker")
	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	@ModuleOption("the password to use when connecting to the broker")
	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isCleanSession() {
		return cleanSession;
	}

	@ModuleOption("whether the client and server should remember state across restarts and reconnects")
	public void setCleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}

	public int getKeepAliveInterval() {
		return keepAliveInterval;
	}

	@ModuleOption("the ping interval in seconds")
	public void setKeepAliveInterval(int keepAliveInterval) {
		this.keepAliveInterval = keepAliveInterval;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	@ModuleOption("the connection timeout in seconds")
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public String getPersistence() {
		return persistence;
	}

	@ModuleOption("'memory' or 'file'")
	public void setPersistence(String persistence) {
		this.persistence = persistence;
	}

	public String getPersistenceDirectory() {
		return persistenceDirectory;
	}

	@ModuleOption("file location when using 'file' persistence")
	public void setPersistenceDirectory(String persistenceDirectory) {
		this.persistenceDirectory = persistenceDirectory;
	}

	@Override
	public String[] profilesToActivate() {
		return new String[] { this.persistence };
	}

}
