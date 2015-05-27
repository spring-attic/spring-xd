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

import org.springframework.xd.dirt.modules.metadata.FileSinkOptionsMetadata.Mode;
import org.springframework.xd.module.options.mixins.FtpConnectionMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Module options for the FTP sink module.
 *
 * @author Franck Marchand
 */
@Mixin({ FtpConnectionMixin.class })
public class FtpSinkOptionsMetadata {

	private int clientMode = 0;

	private String remoteDir = "/";

	private String temporaryRemoteDir = "/";

	private boolean autoCreateDir = true;

	private String remoteFileSeparator = "/";

	private String tmpFileSuffix = ".tmp";

	private Mode mode = Mode.REPLACE;

	private boolean useTemporaryFilename = true;

	public boolean isUseTemporaryFilename() {
		return useTemporaryFilename;
	}

	@ModuleOption("use a temporary filename while transferring the file and rename it to its final name once it's fully transferred")
	public void setUseTemporaryFilename(boolean useTemporaryFilename) {
		this.useTemporaryFilename = useTemporaryFilename;
	}

	public Mode getMode() {
		return mode;
	}

	@ModuleOption("what to do if the file already exists")
	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public String getRemoteDir() {
		return remoteDir;
	}

	@ModuleOption("the remote directory to transfer the files to")
	public void setRemoteDir(String remoteDir) {
		this.remoteDir = remoteDir;
	}

	public String getTemporaryRemoteDir() {
		return temporaryRemoteDir;
	}

	@ModuleOption("temporary remote directory that should be used")
	public void setTemporaryRemoteDir(String temporaryRemoteDir) {
		this.temporaryRemoteDir = temporaryRemoteDir;
	}

	public boolean isAutoCreateDir() {
		return autoCreateDir;
	}

	@ModuleOption("remote directory must be auto created if it does not exist")
	public void setAutoCreateDir(boolean autoCreateDir) {
		this.autoCreateDir = autoCreateDir;
	}

	public String getTmpFileSuffix() {
		return tmpFileSuffix;
	}

	@ModuleOption("extension to use on server side when uploading files")
	public void setTmpFileSuffix(String tmpFileSuffix) {
		this.tmpFileSuffix = tmpFileSuffix;
	}

	public int getClientMode() {
		return clientMode;
	}

	@ModuleOption("client mode to use: 2 for passive mode and 0 for active mode")
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

}
