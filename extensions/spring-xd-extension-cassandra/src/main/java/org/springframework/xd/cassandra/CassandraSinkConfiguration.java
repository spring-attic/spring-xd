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

import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cassandra.core.ConsistencyLevel;
import org.springframework.cassandra.core.RetryPolicy;
import org.springframework.cassandra.core.WriteOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.cassandra.outbound.CassandraMessageHandler;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.integration.transformer.AbstractPayloadTransformer;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;

/**
 * @author Artem Bilan
 */
@Configuration
@EnableIntegration
@Import(CassandraConfiguration.class)
public class CassandraSinkConfiguration {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	@Value("${consistencyLevel:}")
	private ConsistencyLevel consistencyLevel;

	@Value("${retryPolicy:}")
	private RetryPolicy retryPolicy;

	@Value("${ttl}")
	private int ttl;

	@Value("${queryType}")
	private CassandraMessageHandler.Type queryType;

	@Value("${ingestQuery:}")
	private String ingestQuery;

	@Value("${statementExpression:}")
	private String statementExpression;

	@Autowired
	public CassandraOperations template;

	@Bean
	public MessageChannel input() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel toSink() {
		return new DirectChannel();
	}

	@Bean
	@ServiceActivator(inputChannel = "input")
	public MessageHandler toSinkBridgeMessageHandler() {
		AbstractMessageProducingHandler messageHandler;
		if (StringUtils.hasText(this.ingestQuery)) {
			messageHandler = new MessageTransformingHandler(new PayloadToMatrixTransformer(this.ingestQuery));
		}
		else {
			messageHandler = new BridgeHandler();
		}
		messageHandler.setOutputChannel(toSink());
		return messageHandler;
	}

	@Bean
	@ServiceActivator(inputChannel = "toSink")
	public MessageHandler cassandraSinkMessageHandler() {
		CassandraMessageHandler<?> cassandraMessageHandler = new CassandraMessageHandler<>(this.template, this.queryType);
		cassandraMessageHandler.setProducesReply(false);
		if (this.consistencyLevel != null || this.retryPolicy != null || this.ttl > 0) {
			cassandraMessageHandler.setWriteOptions(new WriteOptions(this.consistencyLevel, this.retryPolicy, this.ttl));
		}
		if (StringUtils.hasText(this.ingestQuery)) {
			cassandraMessageHandler.setIngestQuery(this.ingestQuery);
		}
		else if (StringUtils.hasText(this.statementExpression)) {
			Expression expression = PARSER.parseExpression(this.statementExpression);
			cassandraMessageHandler.setStatementExpression(expression);
		}
		return cassandraMessageHandler;
	}

	private static boolean isUuid(String uuid) {
		if (uuid.length() == 36) {
			String[] parts = uuid.split("-");
			if (parts.length == 5) {
				if ((parts[0].length() == 8) && (parts[1].length() == 4) &&
						(parts[2].length() == 4) && (parts[3].length() == 4) &&
						(parts[4].length() == 12)) {
					return true;
				}
			}
		}
		return false;
	}


	private static class PayloadToMatrixTransformer extends AbstractPayloadTransformer<Object, List<List<Object>>> {

		private static final Pattern PATTERN = Pattern.compile(".+\\((.+)\\).+(?:values\\s*\\((.+)\\))");

		private final ObjectMapper objectMapper = new ObjectMapper();

		private final JsonObjectMapper<?, ?> jsonObjectMapper = new Jackson2JsonObjectMapper(this.objectMapper);

		private final List<String> columns = new LinkedList<>();

		private final ISO8601StdDateFormat dateFormat = new ISO8601StdDateFormat();

		public PayloadToMatrixTransformer(String query) {
			Matcher matcher = PATTERN.matcher(query);
			if (matcher.matches()) {
				String[] columns = StringUtils.delimitedListToStringArray(matcher.group(1), ",", " ");
				String[] values = StringUtils.delimitedListToStringArray(matcher.group(2), ",", " ");
				for (int i = 0; i < columns.length; i++) {
					if (values[i].equals("?")) {
						this.columns.add(columns[i]);
					}
				}
			}
			else {
				throw new IllegalArgumentException("Invalid CQL insert query syntax: " + query);
			}
			this.objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
		}

		@Override
		@SuppressWarnings("unchecked")
		protected List<List<Object>> transformPayload(Object payload) throws Exception {
			if (payload instanceof List) {
				return (List<List<Object>>) payload;
			}
			else {
				List<Map<String, Object>> model = this.jsonObjectMapper.fromJson(payload, List.class);
				List<List<Object>> data = new ArrayList<>(model.size());
				for (Map<String, Object> entity : model) {
					List<Object> row = new ArrayList<>(this.columns.size());
					for (String column : this.columns) {
						Object value = entity.get(column);
						if (value instanceof String) {
							String string = (String) value;
							if (this.dateFormat.looksLikeISO8601(string)) {
								synchronized (this.dateFormat) {
									value = this.dateFormat.parse(string);
								}
							}
							if (isUuid(string)) {
								value = UUID.fromString(string);
							}
						}
						row.add(value);
					}
					data.add(row);
				}
				return data;
			}
		}

	}

	@SuppressWarnings("serial")
	private static class ISO8601StdDateFormat extends StdDateFormat {

		@Override
		protected boolean looksLikeISO8601(String dateStr) {
			return super.looksLikeISO8601(dateStr);
		}

	}

}
