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

package org.springframework.xd.integration.hadoop.config;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.data.hadoop.store.DataStoreWriter;
import org.springframework.data.hadoop.store.codec.CodecInfo;
import org.springframework.data.hadoop.store.output.PartitionTextFileWriter;
import org.springframework.data.hadoop.store.output.TextFileWriter;
import org.springframework.data.hadoop.store.strategy.naming.FileNamingStrategy;
import org.springframework.data.hadoop.store.strategy.rollover.RolloverStrategy;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;
import org.springframework.xd.integration.hadoop.partition.MessagePartitionStrategy;

/**
 * A {@link FactoryBean} creating a {@link DataStoreWriter}. Created writer will be either
 * {@link PartitionTextFileWriter} or {@link TextFileWriter} depending whether partition
 * path expression is set.
 *
 * @author Janne Valkealahti
 * @author Gary Russell
 */
public class StoreWriterFactoryBean implements InitializingBean, DisposableBean, FactoryBean<DataStoreWriter<?>>,
		BeanFactoryAware, Lifecycle {

	private volatile DataStoreWriter<?> storeWriter;

	private volatile Configuration configuration;

	private volatile Path basePath;

	private volatile CodecInfo codec;

	private volatile long idleTimeout;

	private volatile long closeTimeout;

	private volatile long flushTimeout;

	private volatile boolean enableSync = false;

	private volatile String inUseSuffix;

	private volatile String inUsePrefix;

	private volatile boolean overwrite = false;

	private volatile String partitionExpression;

	private volatile int fileOpenAttempts;

	private volatile FileNamingStrategy fileNamingStrategy;

	private volatile RolloverStrategy rolloverStrategy;

	private volatile BeanFactory beanFactory;

	private volatile EvaluationContext evaluationContext;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public DataStoreWriter<?> getObject() throws Exception {
		return storeWriter;
	}

	@Override
	public Class<?> getObjectType() {
		return DataStoreWriter.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void destroy() throws Exception {
		storeWriter = null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.evaluationContext == null) {
			this.evaluationContext = IntegrationContextUtils.getEvaluationContext(this.beanFactory);
		}
		if (StringUtils.hasText(partitionExpression)) {
			if (!(evaluationContext instanceof StandardEvaluationContext)) {
				throw new RuntimeException("Expecting evaluationContext of type StandardEvaluationContext but was "
						+ evaluationContext);
			}
			MessagePartitionStrategy<String> partitionStrategy = new MessagePartitionStrategy<String>(
					partitionExpression, (StandardEvaluationContext) evaluationContext);
			PartitionTextFileWriter<Message<?>> writer = new PartitionTextFileWriter<Message<?>>(configuration,
					basePath,
					codec,
					partitionStrategy);
			writer.setIdleTimeout(idleTimeout);
			writer.setCloseTimeout(closeTimeout);
			writer.setFlushTimeout(flushTimeout);
			writer.setSyncable(enableSync);
			writer.setInWritingPrefix(inUsePrefix);
			writer.setInWritingSuffix(inUseSuffix);
			writer.setOverwrite(overwrite);
			writer.setFileNamingStrategyFactory(fileNamingStrategy);
			writer.setRolloverStrategyFactory(rolloverStrategy);
			if (beanFactory != null) {
				writer.setBeanFactory(beanFactory);
			}
			if (fileOpenAttempts > 0) {
				writer.setMaxOpenAttempts(fileOpenAttempts);
			}
			storeWriter = writer;
		}
		else {
			TextFileWriter writer = new TextFileWriter(configuration, basePath, codec);
			writer.setIdleTimeout(idleTimeout);
			writer.setCloseTimeout(closeTimeout);
			writer.setInWritingPrefix(inUsePrefix);
			writer.setInWritingSuffix(inUseSuffix);
			writer.setOverwrite(overwrite);
			writer.setFlushTimeout(flushTimeout);
			writer.setSyncable(enableSync);
			writer.setFileNamingStrategy(fileNamingStrategy);
			writer.setRolloverStrategy(rolloverStrategy);
			if (beanFactory != null) {
				writer.setBeanFactory(beanFactory);
			}
			if (fileOpenAttempts > 0) {
				writer.setMaxOpenAttempts(fileOpenAttempts);
			}
			storeWriter = writer;
		}
		if (storeWriter instanceof InitializingBean) {
			((InitializingBean) storeWriter).afterPropertiesSet();
		}
	}

	@Override
	public boolean isRunning() {
		if (storeWriter instanceof Lifecycle) {
			return ((Lifecycle) storeWriter).isRunning();
		}
		else {
			return false;
		}
	}

	@Override
	public void start() {
		if (storeWriter instanceof Lifecycle) {
			((Lifecycle) storeWriter).start();
		}
	}

	@Override
	public void stop() {
		if (storeWriter instanceof Lifecycle) {
			((Lifecycle) storeWriter).stop();
		}
	}

	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		// used with partition writer spel if set
		this.evaluationContext = evaluationContext;
	}

	/**
	 * Sets the hadoop configuration for the writer.
	 *
	 * @param configuration the new configuration
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Sets the base path for the writer.
	 *
	 * @param basePath the new base path
	 */
	public void setBasePath(Path basePath) {
		this.basePath = basePath;
	}

	/**
	 * Sets the codec for the writer.
	 *
	 * @param codec the new codec
	 */
	public void setCodec(CodecInfo codec) {
		this.codec = codec;
	}

	/**
	 * Sets the idle timeout for the writer.
	 *
	 * @param idleTimeout the new idle timeout
	 */
	public void setIdleTimeout(long idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	/**
	 * Sets the close timeout for the writer.
	 *
	 * @param closeTimeout the new close timeout
	 */
	public void setCloseTimeout(long closeTimeout) {
		this.closeTimeout = closeTimeout;
	}

	/**
	 * Sets the flush timeout for the writer.
	 *
	 * @param flushTimeout the new flush timeout
	 */
	public void setFlushTimeout(long flushTimeout) {
		this.flushTimeout = flushTimeout;
	}

	/**
	 * Enables the syncable flag for the writer.
	 *
	 * @param enableSync the new syncable flag
	 */
	public void setEnableSync(boolean enableSync) {
		this.enableSync = enableSync;
	}

	/**
	 * Sets the in use suffix for the writer.
	 *
	 * @param inUseSuffix the new in use suffix
	 */
	public void setInUseSuffix(String inUseSuffix) {
		this.inUseSuffix = inUseSuffix;
	}

	/**
	 * Sets the in use prefix for the writer.
	 *
	 * @param inUsePrefix the new in use prefix
	 */
	public void setInUsePrefix(String inUsePrefix) {
		this.inUsePrefix = inUsePrefix;
	}

	/**
	 * Sets the file overwrite flag for the writer.
	 *
	 * @param overwrite the new overwrite
	 */
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	/**
	 * Sets the partition expression. This expression is used to determine
	 * if this factory creates an instance of {@link PartitionTextFileWriter}
	 * or {@link TextFileWriter}. Validity of this spel expression
	 * is not checked in this factory, thus any non empty string will
	 * result creation of {@link PartitionTextFileWriter}.
	 *
	 * @param partitionExpression the new partition expression
	 */
	public void setPartitionExpression(String partitionExpression) {
		this.partitionExpression = partitionExpression;
	}

	/**
	 * Sets the file open attempts for the writer.
	 *
	 * @param fileOpenAttempts the new file open attempts
	 */
	public void setFileOpenAttempts(int fileOpenAttempts) {
		this.fileOpenAttempts = fileOpenAttempts;
	}

	/**
	 * Sets the naming strategy.
	 *
	 * @param fileNamingStrategy the new naming strategy
	 */
	public void setNamingStrategy(FileNamingStrategy fileNamingStrategy) {
		this.fileNamingStrategy = fileNamingStrategy;
	}

	/**
	 * Sets the rollover strategy.
	 *
	 * @param rolloverStrategy the new rollover strategy
	 */
	public void setRolloverStrategy(RolloverStrategy rolloverStrategy) {
		this.rolloverStrategy = rolloverStrategy;
	}

}
