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

package org.springframework.xd.mail;

import static org.springframework.xd.mail.MailProtocol.imap;
import static org.springframework.xd.mail.MailProtocol.imaps;

import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.springframework.xd.module.options.mixins.MaxMessagesDefaultOneMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;
import org.springframework.xd.module.options.validation.Exclusives;


/**
 * Captures options for the {@code mail} source module.
 *
 * @author Eric Bottard
 * @author Gary Russell
 * @author Ilayaperumal Gopinathan
 */
@Mixin({MailServerMixin.class, MaxMessagesDefaultOneMixin.class})
public class MailSourceOptionsMetadata implements ProfileNamesProvider {

	private int fixedDelay = 60;

	private MailProtocol protocol = imap;

	private boolean usePolling = false;

	private boolean delete = true;

	private boolean markAsRead = false;

	private String charset = "UTF-8";

	private String folder = "INBOX";

	private String expression = "true";

	private String defaultProperties = null;

	private String properties = null;

	private String propertiesFile = null;

	private static final String IMAPS_PROPERTIES = "mail.imap.socketFactory.class=javax.net.ssl.SSLSocketFactory," +
			"mail.imap.socketFactory.fallback=false," +
			"mail.store.protocol=imaps";


	@NotNull
	public String getExpression() {
		return expression;
	}

	@ModuleOption("a SpEL expression which filters which mail messages will be processed (non polling imap only)")
	public void setExpression(String expression) {
		this.expression = expression;
	}

	@NotNull
	public String getFolder() {
		return folder;
	}

	@ModuleOption("the folder to take emails from")
	public void setFolder(String folder) {
		this.folder = folder;
	}

	@NotNull
	public String getCharset() {
		return charset;
	}

	@ModuleOption("the charset used to transform the body of the incoming emails to Strings")
	public void setCharset(String charset) {
		this.charset = charset;
	}


	public boolean isDelete() {
		return delete;
	}


	public boolean isMarkAsRead() {
		return markAsRead;
	}

	@ModuleOption("whether to delete the emails once they’ve been fetched")
	public void setDelete(boolean delete) {
		this.delete = delete;
	}


	@ModuleOption("whether to mark emails as read once they’ve been fetched")
	public void setMarkAsRead(boolean markAsRead) {
		this.markAsRead = markAsRead;
	}

	@Min(0)
	public int getFixedDelay() {
		return fixedDelay;
	}

	@NotNull
	public MailProtocol getProtocol() {
		return protocol;
	}

	public boolean isUsePolling() {
		return usePolling;
	}

	@ModuleOption("comma separated JavaMail property values")
	public void setProperties(String properties) {
		this.properties = properties;
	}

	public String getProperties() {
		return this.properties;
	}

	@ModuleOption("file to load the JavaMail properties")
	public void setPropertiesFile(String propertiesFile) {
		this.propertiesFile = propertiesFile;
	}

	public String getPropertiesFile() {
		return this.propertiesFile;
	}

	public String getDefaultProperties() {
		return defaultProperties;
	}

	@Override
	public String[] profilesToActivate() {
		return (usePolling) ? new String[] {"use-polling"} : new String[] {"use-idle"};
	}

	@ModuleOption("the polling interval used for looking up messages (s)")
	public void setFixedDelay(int fixedDelay) {
		this.fixedDelay = fixedDelay;
	}

	@ModuleOption("the protocol to use to retrieve messages")
	public void setProtocol(MailProtocol protocol) {
		this.protocol = protocol;
		if (protocol == imaps) {
			this.defaultProperties = IMAPS_PROPERTIES;
		}
	}

	@ModuleOption("whether to use polling or not (no polling works with imap(s) only)")
	public void setUsePolling(boolean usePolling) {
		this.usePolling = usePolling;
	}

	@AssertTrue(message = "usePolling=false is only supported with imap(s)")
	private boolean isUsePollingValid() {
		if (!usePolling) {
			return protocol == imap || protocol == imaps;
		}
		else {
			return true;
		}
	}

	@AssertFalse(message = "'properties' and 'propertiesFile' are mutually exclusive")
	private boolean isInvalid() {
		return Exclusives.strictlyMoreThanOne(properties != null, propertiesFile != null);
	}
}
