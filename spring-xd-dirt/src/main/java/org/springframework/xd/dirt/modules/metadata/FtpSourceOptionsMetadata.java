/*
 * Copyright 2015 the original author or authors.
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

/**
 * Module options for FTP source module.
 *
 * @author Franck MARCHAND
 */
public class FtpSourceOptionsMetadata {

	private String host = "localhost";

	private int port = 21;

	private String username;

	private String password;

	private int clientMode = 0;

	private String remoteDir;

	private boolean deleteRemoteFiles = false;

	private String localDir = "/tmp/xd/ftp";

	private boolean autoCreateLocalDir = true;

	private String tmpFileSuffix = ".tmp";

	private int fixedRate = 1000;

	private String filenamePattern = "*";

	private String remoteFileSeparator = "/";

	private boolean preserveTimestamp = true;

	public String getHost() {
		return host;
	}

	@ModuleOption("the remote host to connect to")
	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	@ModuleOption("the remote port to connect to")
	public void setPort(int port) {
		this.port = port;
	}

	@NotBlank
	public String getUsername() {
		return username;
	}

	@ModuleOption("the username to use")
	public void setUsername(String user) {
		this.username = user;
	}

	public String getPassword() {
		return password;
	}

	@ModuleOption("the password for the provided user")
	public void setPassword(String password) {
		this.password = password;
	}

	@NotBlank
	public String getRemoteDir() {
		return remoteDir;
	}

	@ModuleOption("the remote directory to transfer the files from")
	public void setRemoteDir(String remoteDir) {
		this.remoteDir = remoteDir;
	}

	public boolean isDeleteRemoteFiles() {
		return deleteRemoteFiles;
	}

	@ModuleOption("delete remote files after transfer")
	public void setDeleteRemoteFiles(boolean deleteRemoteFiles) {
		this.deleteRemoteFiles = deleteRemoteFiles;
	}

	public String getLocalDir() {
		return localDir;
	}

	@ModuleOption("set the local directory the remote files are transferred to")
	public void setLocalDir(String localDir) {
		this.localDir = localDir;
	}

	public boolean isAutoCreateLocalDir() {
		return autoCreateLocalDir;
	}

	@ModuleOption("local directory must be auto created if it does not exist")
	public void setAutoCreateLocalDir(boolean autoCreateLocalDir) {
		this.autoCreateLocalDir = autoCreateLocalDir;
	}

	public String getTmpFileSuffix() {
		return tmpFileSuffix;
	}

	@ModuleOption("extension to use when downloading files")
	public void setTmpFileSuffix(String tmpFileSuffix) {
		this.tmpFileSuffix = tmpFileSuffix;
	}

	public int getFixedRate() {
		return fixedRate;
	}

	@ModuleOption("fixed delay in SECONDS to poll the remote directory")
	public void setFixedRate(int fixedRate) {
		this.fixedRate = fixedRate;
	}

	public String getFilenamePattern() {
		return filenamePattern;
	}

	@ModuleOption("simple filename pattern to apply to the filter")
	public void setFilenamePattern(String pattern) {
		this.filenamePattern = pattern;
	}


	public int getClientMode() {
		return clientMode;
	}

	@ModuleOption("client mode to use : 2 for passive mode and 0 for active mode")
	public void setClientMode(int clientMode) {
		this.clientMode = clientMode;
	}

	public String getRemoteFileSeparator() {
		return remoteFileSeparator;
	}

	@ModuleOption("file separator to use on the remote side")
	public void setRemoteFileSeparator(String remoteFileSeparator) {
		this.remoteFileSeparator = remoteFileSeparator;
	}

	public boolean isPreserveTimestamp() {
		return preserveTimestamp;
	}

	@ModuleOption("preserve timestamp")
	public void setPreserveTimestamp(boolean preserveTimestamp) {
		this.preserveTimestamp = preserveTimestamp;
	}
}
