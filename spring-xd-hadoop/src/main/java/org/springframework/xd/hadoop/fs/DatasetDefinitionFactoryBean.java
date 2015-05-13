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

package org.springframework.xd.hadoop.fs;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.kitesdk.data.PartitionStrategy;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.hadoop.store.StoreException;
import org.springframework.data.hadoop.store.dataset.DatasetDefinition;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

/**
 * FactoryBean that creates a default DatasetDefinition
 *
 * @author Thomas Risberg
 */
public class DatasetDefinitionFactoryBean implements InitializingBean, FactoryBean<DatasetDefinition> {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private String format;

	private Boolean allowNullValues;

	private String partitionPath;

	private int writerCacheSize;

	private String compressionType;

	private DatasetDefinition datasetDefinition;


	public void setFormat(String format) {
		this.format = format;
	}

	public void setAllowNullValues(Boolean allowNullValues) {
		this.allowNullValues = allowNullValues;
	}

	public void setPartitionPath(String partitionPath) {
		this.partitionPath = partitionPath;
	}

	public void setWriterCacheSize(int writerCacheSize) {
		this.writerCacheSize = writerCacheSize;
	}

	public void setCompressionType(String compressionType) {
		this.compressionType = compressionType;
	}

	@Override
	public DatasetDefinition getObject() throws Exception {
		return datasetDefinition;
	}

	@Override
	public Class<?> getObjectType() {
		return DatasetDefinition.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		logger.info("DatasetDefinition properties: ");
		logger.info("  allowNulValues: " + allowNullValues);
		logger.info("          format: " + format);
		logger.info("   partitionPath: " + partitionPath);
		logger.info(" writerCacheSize: " + writerCacheSize);
		logger.info(" compressionType: " + compressionType);
		datasetDefinition = new DatasetDefinition(allowNullValues, format);
		if (StringUtils.hasText(partitionPath)) {
			datasetDefinition.setPartitionStrategy(parsePartitionExpression(partitionPath));
		}
		if (writerCacheSize > 0) {
			datasetDefinition.setWriterCacheSize(writerCacheSize);
		}
		if (StringUtils.hasText(compressionType)) {
			datasetDefinition.setCompressionType(compressionType);
		}
	}

	private static PartitionStrategy parsePartitionExpression(String expression) {

		List<String> expressions = Arrays.asList(expression.split("/"));

		ExpressionParser parser = new SpelExpressionParser();
		PartitionStrategy.Builder psb = new PartitionStrategy.Builder();
		StandardEvaluationContext ctx = new StandardEvaluationContext(psb);
		for (String expr : expressions) {
			try {
				Expression e = parser.parseExpression(expr);
				psb = e.getValue(ctx, PartitionStrategy.Builder.class);
			}
			catch (SpelParseException spe) {
				if (!expr.trim().endsWith(")")) {
					throw new StoreException("Invalid partitioning expression '" + expr
							+ "' -  did you forget the closing parenthesis?", spe);
				}
				else {
					throw new StoreException("Invalid partitioning expression '" + expr + "'!", spe);
				}
			}
			catch (SpelEvaluationException see) {
				throw new StoreException("Invalid partitioning expression '" + expr + "' - failed evaluation!", see);
			}
			catch (NullPointerException npe) {
				throw new StoreException("Invalid partitioning expression '" + expr + "' - was evaluated to null!", npe);
			}
		}
		return psb.build();
	}

}
