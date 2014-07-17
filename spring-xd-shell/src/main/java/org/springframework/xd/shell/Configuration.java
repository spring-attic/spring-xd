/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.shell;

import java.util.Date;
import java.util.TimeZone;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.xd.rest.domain.util.TimeUtils;
import org.springframework.xd.shell.command.ConfigCommands;
import org.springframework.xd.shell.util.CommonUtils;

/**
 * Encapsulates various configuration properties for the shell such as {@link Target}
 * and the configurable {@link TimeZone}.
 *
 * @author Gunnar Hillert
 * @since 1.0
 *
 */
@Component
public class Configuration {

	private Target target;

	private TimeZone clientTimeZone;

	/**
	 * Constructor initializing the default timezone using {@link TimeUtils#getJvmTimeZone()}
	 */
	public Configuration() {
		clientTimeZone = TimeUtils.getJvmTimeZone();
	}

	/**
	 * Return the {@link Target} which encapsulates not only the Target URI but also success/error messages + status.
	 *
	 * @return Should never be null. Initialized by {@link ConfigCommands#afterPropertiesSet()}.
	 */
	public Target getTarget() {
		return target;
	}

	/**
	 * Set the XD Admin Server {@link Target}.
	 *
	 * @param target Must not be null.
	 */
	public void setTarget(Target target) {
		Assert.notNull(target, "The provided target must not be null.");
		this.target = target;
	}

	/**
	 * @return the local {@link TimeZone}.
	 */
	public TimeZone getClientTimeZone() {
		return clientTimeZone;
	}

	/**
	 * If not set, the used {@link TimeZone} will default to {@link TimeUtils#getJvmTimeZone()}.
	 * @param clientTimeZone Must not be null
	 */
	public void setClientTimeZone(TimeZone clientTimeZone) {
		Assert.notNull(clientTimeZone, "The provided timeZone must not be null.");
		this.clientTimeZone = clientTimeZone;
	}

	/**
	 * Returns a String formatted Date/Time using the configured {@link Configuration#clientTimeZone}.
	 *
	 * @param date Can be null
	 * @return Should never return null.
	 */
	public String getLocalTime(Date date) {
		return CommonUtils.getLocalTime(date, this.clientTimeZone);
	}

}
