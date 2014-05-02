/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.integration.hadoop.outbound;

import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.SmartLifecycle;
import org.springframework.data.hadoop.store.PartitionDataStoreWriter;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.util.Assert;

/**
 * Spring Integration {@code MessageHandler} handling {@code Message} writing into hdfs using {@code DataStoreWriter}.
 * 
 * @author Rodrigo Meneses
 * 
 */
// TODO: see If I can extend the HdfsStoreMessageHandler
public class PartitionHdfsStoreMessageHandler extends AbstractMessageHandler implements SmartLifecycle {

	private static final Log log = LogFactory.getLog(PartitionHdfsStoreMessageHandler.class);

	private volatile boolean autoStartup = true;

	private volatile int phase = 0;

	private volatile boolean running;

	private final ReentrantLock lifecycleLock = new ReentrantLock();

	private PartitionDataStoreWriter<String> partitionStoreWriter;

	public PartitionDataStoreWriter<String> getPartitionStoreWriter() {
		return partitionStoreWriter;
	}

	public void setPartitionExpressionGenerator(String partitionExpressionGenerator) {
		this.partitionExpressionGenerator = partitionExpressionGenerator;
	}

	private String partitionExpressionGenerator;

	@Override
	public final boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public final int getPhase() {
		return this.phase;
	}

	@Override
	public final boolean isRunning() {
		this.lifecycleLock.lock();
		try {
			return this.running;
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	@Override
	public final void start() {
		this.lifecycleLock.lock();
		try {
			if (!this.running) {
				this.doStart();
				this.running = true;
				if (log.isInfoEnabled()) {
					log.info("started " + this);
				}
				else {
					if (log.isDebugEnabled()) {
						log.debug("already started " + this);
					}
				}
			}
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	@Override
	public final void stop() {
		this.lifecycleLock.lock();
		try {
			if (this.running) {
				this.doStop();
				this.running = false;
				if (log.isInfoEnabled()) {
					log.info("stopped " + this);
				}
			}
			else {
				if (log.isDebugEnabled()) {
					log.debug("already stopped " + this);
				}
			}
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	@Override
	public final void stop(Runnable callback) {
		this.lifecycleLock.lock();
		try {
			this.stop();
			callback.run();
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Expression expression = new SpelExpressionParser().parseExpression(partitionExpressionGenerator);
		Object dirNameObject = expression.getValue(message);
		log.debug("Expression evalute to " + dirNameObject);
		if (dirNameObject != null && dirNameObject instanceof String) {
			String dirName = (String) dirNameObject;

			try {
				Object payload = message.getPayload();
				if (payload instanceof String) {
					log.debug("Calling to " + payload + " in partition " + dirName);
					partitionStoreWriter.writeToPartition(dirName, (String) payload);
				}
				else {
					throw new MessageHandlingException(message,
							"message not a String");
				}
			}
			catch (Exception e) {
				throw new MessageHandlingException(message,
						"failed to write Message payload to HDFS", e);
			}

		}
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		Assert.notNull(partitionStoreWriter, "Data Writer must be set");
	}

	/**
	 * Sets the auto startup.
	 * 
	 * @param autoStartup the new auto startup
	 * @see SmartLifecycle
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * Sets the phase.
	 * 
	 * @param phase the new phase
	 * @see SmartLifecycle
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * Sets the store writer.
	 * 
	 * @param storeWriter the new store writer
	 */
	public void setPartitionStoreWriter(PartitionDataStoreWriter<String> partitionStoreWriter) {
		this.partitionStoreWriter = partitionStoreWriter;
	}

	/**
	 * Subclasses may override this method with the start behaviour. This method will be invoked while holding the
	 * {@link #lifecycleLock}.
	 */
	protected void doStart() {
	};

	/**
	 * Subclasses may override this method with the stop behaviour. This method will be invoked while holding the
	 * {@link #lifecycleLock}.
	 */
	protected void doStop() {
		try {
			partitionStoreWriter.close();
		}
		catch (Exception e) {
			log.error("Error closing writer", e);
		}
	};


	public String getPartitionExpressionGenerator() {
		return partitionExpressionGenerator;
	}

	public void setDirectoryExpressionGenerator(String partitionExpressionGenerator) {
		this.partitionExpressionGenerator = partitionExpressionGenerator;
	}

}
