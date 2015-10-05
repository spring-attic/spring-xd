/*
 * Copyright 2014-2015 the original author or authors.
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

import javax.validation.constraints.AssertTrue;

import org.hibernate.validator.constraints.NotBlank;

import org.springframework.util.StringUtils;
import org.springframework.xd.module.options.mixins.MaxMessagesDefaultUnlimitedMixin;
import org.springframework.xd.module.options.mixins.PeriodicTriggerMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;


/**
 * Module options for SFTP source module.
 *
 * @author Ilayaperumal Gopinathan
 * @author Eric Bottard
 * @author Gary Russell
 */

@Mixin({ PeriodicTriggerMixin.class, FileAsRefMixin.class, MaxMessagesDefaultUnlimitedMixin.class })
public class SftpSourceOptionsMetadata implements ProfileNamesProvider {

	private static final String ACCEPT_ALL_FILES = "accept-all-files";

	private static final String SIMPLE_PATTERN_FILTER = "use-filename-simple-pattern";

	private static final String REGEX_PATTERN_FILTER = "use-filename-regex-pattern";

	private String host = "localhost";

	private int port = 22;

	private String user;

	private String password = "";

	private String privateKey = "";

	private String passPhrase = "";

	private String remoteDir;

	private boolean deleteRemoteFiles = false;

	private String localDir = "/tmp/xd/output";

	private boolean autoCreateLocalDir = true;

	private String tmpFileSuffix = ".tmp";

	private int fixedDelay = 1;

	private String pattern = null;

	private String regexPattern = null;

	private boolean allowUnknownKeys = false;

	private String knownHostsExpression = null;

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
	public String getUser() {
		return user;
	}

	@ModuleOption("the username to use")
	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	@ModuleOption("the password for the provided user")
	public void setPassword(String password) {
		this.password = password;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	@ModuleOption("the private key location (a valid Spring Resource URL)")
	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

	public String getPassPhrase() {
		return passPhrase;
	}

	@ModuleOption("the passphrase to use")
	public void setPassPhrase(String passPhrase) {
		this.passPhrase = passPhrase;
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

	@ModuleOption("if local directory must be auto created if it does not exist")
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

	public int getFixedDelay() {
		return fixedDelay;
	}

	@ModuleOption("fixed delay in SECONDS to poll the remote directory")
	public void setFixedDelay(int fixedDelay) {
		this.fixedDelay = fixedDelay;
	}

	public String getRegexPattern() {
		return regexPattern;
	}

	@ModuleOption("filename regex pattern to apply to the filter")
	public void setRegexPattern(String regexPattern) {
		this.regexPattern = regexPattern;
	}

	public String getPattern() {
		return pattern;
	}

	@ModuleOption("simple filename pattern to apply to the filter")
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public boolean isAllowUnknownKeys() {
		return this.allowUnknownKeys;
	}

	@ModuleOption("true to allow connecting to a host with an unknown or changed key")
	public void setAllowUnknownKeys(boolean allowUnknownKeys) {
		this.allowUnknownKeys = allowUnknownKeys;
	}

	public String getKnownHostsExpression() {
		return this.knownHostsExpression;
	}

	@ModuleOption("a SpEL expresssion location of known hosts file; required if 'allowUnknownKeys' is false; "
			+ "examples: systemProperties[\"user.home\"]+\"/.ssh/known_hosts\", \"/foo/bar/known_hosts\"")
	public void setKnownHostsExpression(String knownHostsExpression) {
		this.knownHostsExpression = knownHostsExpression;
	}

	@AssertTrue(message = "Use ('privateKey' AND 'passphrase') OR 'password' to specify credentials")
	boolean isEitherPasswordOrPrivateKeyAndPassPhrase() {
		if (!StringUtils.hasText(password)) {
			return StringUtils.hasText(privateKey) && StringUtils.hasText(passPhrase);
		}
		return !StringUtils.hasText(privateKey) && !StringUtils.hasText(passPhrase);
	}

	@Override
	public String[] profilesToActivate() {
		if (this.regexPattern != null) {
			return new String[] { REGEX_PATTERN_FILTER };
		}
		else if (this.pattern != null) {
			return new String[] { SIMPLE_PATTERN_FILTER };
		}
		return new String[] { ACCEPT_ALL_FILES };
	}
}
