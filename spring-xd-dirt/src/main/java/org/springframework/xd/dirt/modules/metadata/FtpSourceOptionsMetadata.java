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

import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotBlank;

import org.springframework.xd.module.options.mixins.FtpConnectionMixin;
import org.springframework.xd.module.options.mixins.MaxMessagesDefaultUnlimitedMixin;
import org.springframework.xd.module.options.mixins.PeriodicTriggerMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Module options for FTP source module.
 *
 * @author Franck Marchand
 * @author Eric Bottard
 */
@Mixin({ FtpConnectionMixin.class, PeriodicTriggerMixin.class, FileAsRefMixin.class,
	MaxMessagesDefaultUnlimitedMixin.class })
public class FtpSourceOptionsMetadata {

	private int clientMode = 0;

	private String remoteDir = "/";

	private boolean deleteRemoteFiles = false;

	private String localDir = "/tmp/xd/ftp";

	private boolean autoCreateLocalDir = true;

	private String tmpFileSuffix = ".tmp";

	private int fixedDelay = 1;

	private String filenamePattern = "*";

	private String remoteFileSeparator = "/";

	private boolean preserveTimestamp = true;

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

	@NotBlank
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

	@Min(0)
	public int getFixedDelay() {
		return fixedDelay;
	}

	@ModuleOption("the rate at which to poll the remote directory")
	public void setFixedDelay(int fixedDelay) {
		this.fixedDelay = fixedDelay;
	}

	@NotBlank
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

	@NotBlank
	public String getRemoteFileSeparator() {
		return remoteFileSeparator;
	}

	@ModuleOption("file separator to use on the remote side")
	public void setRemoteFileSeparator(String remoteFileSeparator) {
		this.remoteFileSeparator = remoteFileSeparator;
	}

	@ModuleOption("whether to preserve the timestamp of files retrieved")
	public void setPreserveTimestamp(boolean preserveTimestamp) {
		this.preserveTimestamp = preserveTimestamp;
	}

	public boolean isPreserveTimestamp() {
		return preserveTimestamp;
	}

}
