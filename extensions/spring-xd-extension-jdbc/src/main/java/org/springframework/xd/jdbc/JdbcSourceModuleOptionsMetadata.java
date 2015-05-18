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

package org.springframework.xd.jdbc;

import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotBlank;

import org.springframework.xd.module.options.mixins.MaxMessagesDefaultOneMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;

/**
 * Captures options for the {@code jdbc} source module.
 *
 * @author Eric Bottard
 * @author Thomas Risberg
 * @author Gary Russell
 */
@Mixin({ JdbcConnectionMixin.class, JdbcConnectionPoolMixin.class, MaxMessagesDefaultOneMixin.class })
public class JdbcSourceModuleOptionsMetadata implements ProfileNamesProvider {

	private static final String[] USE_SPLITTER = new String[] { "use-splitter" };

	private static final String[] DONT_USE_SPLITTER = new String[] { "dont-use-splitter" };


	private String query;

	private String update;

	private int fixedDelay = 5;

	private boolean split = true;

	private int maxRowsPerPoll = 0;

	@NotBlank
	public String getQuery() {
		return query;
	}

	@ModuleOption("an SQL select query to execute to retrieve new messages when polling")
	public void setQuery(String query) {
		this.query = query;
	}

	public String getUpdate() {
		return update;
	}

	@ModuleOption("an SQL update statement to execute for marking polled messages as 'seen'")
	public void setUpdate(String update) {
		this.update = update;
	}

	@Min(1)
	public int getFixedDelay() {
		return fixedDelay;
	}

	@ModuleOption("how often to poll for new messages (s)")
	public void setFixedDelay(int fixedDelay) {
		this.fixedDelay = fixedDelay;
	}

	public boolean isSplit() {
		return split;
	}

	@ModuleOption("whether to split the SQL result as individual messages")
	public void setSplit(boolean split) {
		this.split = split;
	}

	public int getMaxRowsPerPoll() {
		return maxRowsPerPoll;
	}

	@ModuleOption("max numbers of rows to process for each poll")
	public void setMaxRowsPerPoll(int maxRowsPerPoll) {
		this.maxRowsPerPoll = maxRowsPerPoll;
	}

	@Override
	public String[] profilesToActivate() {
		return split ? USE_SPLITTER : DONT_USE_SPLITTER;
	}
}
