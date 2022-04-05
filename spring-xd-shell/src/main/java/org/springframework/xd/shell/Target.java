/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.shell;

import java.net.URI;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Encapsulates various data points related to the Admin Server Target, such as target URI, success/error state,
 * exception messages that may have occurred.
 *
 * @author Gunnar Hillert
 * @since 1.0
 *
 */
public class Target {

	public enum TargetStatus {
		SUCCESS, ERROR
	}

	public class Credentials {

		String username;

		String password;

		// for serialization/persistence
		public Credentials() {
		}

		public Credentials(String username, String password) {
			this.username = username;
			this.password = password;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getDisplayableContents() {
			return "[username='" + username +", password=****']";
		}
	}

	public static final String DEFAULT_SCHEME = "http";

	public static final String DEFAULT_HOST = "localhost";

	public static final int DEFAULT_PORT = 9393;

	public static final String DEFAULT_USERNAME = "";

	public static final String DEFAULT_SPECIFIED_PASSWORD = "";

	public static final String DEFAULT_UNSPECIFIED_PASSWORD = "__NULL__";

	public static final String DEFAULT_TARGET = DEFAULT_SCHEME + "://" + DEFAULT_HOST + ":" + DEFAULT_PORT + "/";

	private final URI targetUri;

	private final Credentials targetCredentials;

	private Exception targetException;

	private String targetResultMessage;

	private TargetStatus status;

	/**
	 * Construct a new Target. The passed in <code>targetUriAsString</code> String parameter will be converted to a {@link URI}.
	 * This method allows for providing a username and password for authentication.
	 *
	 * @param targetUriAsString Must not be empty
	 * @param targetUsername    May be empty, if access is unauthenticated
	 * @param targetPassword    May be empty
	 * @throws IllegalArgumentException if the given string violates RFC 2396.
	 */
	public Target(String targetUriAsString, String targetUsername, String targetPassword) {
		Assert.hasText(targetUriAsString, "The provided targetUriAsString must neither be null nor empty.");
		this.targetUri = URI.create(targetUriAsString);
		if (StringUtils.isEmpty(targetUsername)) {
			this.targetCredentials = null;
		} else {
			this.targetCredentials = new Credentials(targetUsername, targetPassword);
		}
	}

	/**
	 * Construct a new Target. The passed in <code>targetUriAsString</code> String parameter will be converted to a {@link URI}.
	 *
	 * @param targetUriAsString Must not be empty
	 * @throws IllegalArgumentException if the given string violates RFC 2396
	 */
	public Target(String targetUriAsString) {
		this(targetUriAsString, null, null);
	}

	/**
	 * Return the target status, which is either Success or Error.
	 *
	 * @return The {@link TargetStatus}. May be null.
	 */
	public TargetStatus getStatus() {
		return status;
	}

	/**
	 * If during targeting an error occurred, the resulting {@link Exception} is made available for further
	 * introspection.
	 *
	 * @return If present, returns the Exception, otherwise null is returned.
	 */
	public Exception getTargetException() {
		return targetException;
	}

	/**
	 * Provides a result message indicating whether the provide {@link #getTargetUri()} was successfully targeted or
	 * not.
	 *
	 * @return The formatted result message.
	 */
	public String getTargetResultMessage() {
		return targetResultMessage;
	}

	/**
	 * @return The Target Uri. Will never be null.
	 */
	public URI getTargetUri() {
		return targetUri;
	}

	/**
	 * Returns the target URI as a String.
	 *
	 * @return Never null and will always return a valid URI value
	 */
	public String getTargetUriAsString() {
		return targetUri.toString();
	}

	/**
	 * Returns the target credentials
	 *
	 * @return The target credentials. May be null if there is no authentication
	 */
	public Credentials getTargetCredentials() {
		return targetCredentials;
	}

	/**
	 * Sets the exception in case an error occurred during targeting. Will also set the respective {@link TargetStatus}
	 * to {@link TargetStatus#ERROR}.
	 *
	 * @param targetException Must not be null.
	 */
	public void setTargetException(Exception targetException) {
		Assert.notNull(targetException, "The provided targetException must not be null.");
		this.targetException = targetException;
		this.status = TargetStatus.ERROR;
	}

	/**
	 * Set the result messages indicating the success or failure while targeting the Spring XD Admin Server.
	 *
	 * @param targetResultMessage Must not be empty.
	 */
	public void setTargetResultMessage(String targetResultMessage) {
		Assert.hasText(targetResultMessage, "The provided targetResultMessage must neither be null nor empty.");
		this.targetResultMessage = targetResultMessage;
	}

	@Override
	public String toString() {
		return "Target [targetUri=" + targetUri + ", targetException=" + targetException + ", targetResultMessage="
				+ targetResultMessage + ", status=" + status + "]";
	}

}
