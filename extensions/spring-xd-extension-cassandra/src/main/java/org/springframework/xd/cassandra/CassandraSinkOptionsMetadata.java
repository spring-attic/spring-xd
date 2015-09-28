/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.cassandra;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.springframework.cassandra.core.ConsistencyLevel;
import org.springframework.cassandra.core.RetryPolicy;
import org.springframework.integration.cassandra.outbound.CassandraMessageHandler;
import org.springframework.util.StringUtils;
import org.springframework.xd.cassandra.mixins.CassandraConnectionMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * @author Artem Bilan
 */
@Mixin(CassandraConnectionMixin.class)
public class CassandraSinkOptionsMetadata {

	private ConsistencyLevel consistencyLevel;

	private RetryPolicy retryPolicy;

	private int ttl;

	private CassandraMessageHandler.Type queryType = CassandraMessageHandler.Type.INSERT;

	private String ingestQuery;

	private String statementExpression;

	@ModuleOption("the consistencyLevel option of WriteOptions")
	public void setConsistencyLevel(ConsistencyLevel consistencyLevel) {
		this.consistencyLevel = consistencyLevel;
	}

	@ModuleOption("the retryPolicy  option of WriteOptions")
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
	}

	@ModuleOption("the time-to-live option of WriteOptions")
	public void setTtl(int ttl) {
		this.ttl = ttl;
	}

	@ModuleOption("the queryType for Cassandra Sink")
	public void setQueryType(CassandraMessageHandler.Type queryType) {
		this.queryType = queryType;
	}

	@ModuleOption("the ingest Cassandra query")
	public void setIngestQuery(String ingestQuery) {
		this.ingestQuery = ingestQuery;
	}

	@ModuleOption("the expression in Cassandra query DSL style")
	public void setStatementExpression(String statementExpression) {
		this.statementExpression = statementExpression;
	}

	public ConsistencyLevel getConsistencyLevel() {
		return this.consistencyLevel;
	}

	public RetryPolicy getRetryPolicy() {
		return this.retryPolicy;
	}

	@Min(0)
	public int getTtl() {
		return this.ttl;
	}

	@NotNull
	public CassandraMessageHandler.Type getQueryType() {
		return queryType;
	}

	public String getIngestQuery() {
		return this.ingestQuery;
	}

	public String getStatementExpression() {
		return this.statementExpression;
	}

	@AssertTrue(message = "'ingestQuery' and 'statementExpression' are mutually exclusive")
	private boolean isInvalid() {
		return !StringUtils.hasText(this.ingestQuery) || !StringUtils.hasText(this.statementExpression);
	}

}
