/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.data.hadoop.store.output;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import org.springframework.core.task.TaskExecutor;
import org.springframework.data.hadoop.store.DataWriter;
import org.springframework.data.hadoop.store.EntityWriter;
import org.springframework.data.hadoop.store.StrategiesStorage;
import org.springframework.data.hadoop.store.support.EntityObjectSupport;
import org.springframework.data.hadoop.store.support.PollingTaskSupport;
import org.springframework.scheduling.TaskScheduler;

/**
 * Base {@code EntityWriter} implementation sharing common functionality.
 * 
 * @param <E> Type of an entity for the writer
 * @author Janne Valkealahti
 * 
 */
public abstract class AbstractEntityWriter<E> extends EntityObjectSupport implements EntityWriter<E> {

	private final static Log log = LogFactory.getLog(AbstractEntityWriter.class);

	/** Writer for the storage */
	private volatile DataWriter writer;

	/** Caching to avoid casting */
	protected volatile StrategiesStorage strategiesStorage;

	/** Lock for writer */
	protected final static Object lock = new Object();

	/** Poller checking idle timeouts */
	private IdleTimeoutPoller idlePoller;

	/**
	 * In millis last time we wrote. We explicitly use negative value to indicate reset state because we can't use long
	 * max value which would flip if adding something. We reset this state when idle timeout happens so that we can wait
	 * next write.
	 */
	private volatile long lastWrite = Long.MIN_VALUE;

	/** In millis an idle timeout for write. */
	private volatile long idleTimeout;

	/**
	 * Instantiates a new abstract data writer.
	 * 
	 * @param storage the storage
	 * @param configuration the configuration
	 * @param path the path
	 */
	protected AbstractEntityWriter(StrategiesStorage storage, Configuration configuration, Path path) {
		super(storage, configuration, path);
		this.strategiesStorage = storage;
	}

	@Override
	public void open() throws IOException {
		synchronized (lock) {
			if (writer == null) {
				writer = getStorage().getDataWriter();
			}
		}
	}

	@Override
	public void write(E entity) throws IOException {
		synchronized (lock) {
			open();
			if (strategiesStorage.checkStrategies()) {
				close();
				open();
			}
			writer.write(convert(entity));
			strategiesStorage.reportSizeAware(writer.getPosition());
			lastWrite = System.currentTimeMillis();
		}
	}

	@Override
	public void flush() throws IOException {
		synchronized (lock) {
			if (writer != null) {
				writer.flush();
			}
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (lock) {
			if (writer != null) {
				writer.close();
				writer = null;
			}
		}
	}

	@Override
	protected void onInit() throws Exception {
		// if we have timeout, enable polling by creating it
		if (idleTimeout > 0) {
			idlePoller = new IdleTimeoutPoller(getTaskScheduler(), getTaskExecutor(), TimeUnit.SECONDS, 10);
			idlePoller.init();
		}
	}

	@Override
	protected void doStart() {
		if (idlePoller != null) {
			idlePoller.start();
		}
	}

	@Override
	protected void doStop() {
		if (idlePoller != null) {
			idlePoller.stop();
		}
		idlePoller = null;
	}

	/**
	 * Sets the idle timeout.
	 * 
	 * @param idleTimeout the new idle timeout
	 */
	public void setIdleTimeout(long idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	/**
	 * Convert an entity into byte array. Subclass needs to override this method to introduce conversion logic.
	 * 
	 * @param entity the entity
	 * @return the byte[] to be written
	 */
	protected abstract byte[] convert(E entity);

	/**
	 * Poller which checks idle timeout by last write and closes a writer if timeout has occurred.
	 */
	private class IdleTimeoutPoller extends PollingTaskSupport<Boolean> {

		public IdleTimeoutPoller(TaskScheduler taskScheduler, TaskExecutor taskExecutor, TimeUnit unit,
				long duration) {
			super(taskScheduler, taskExecutor, unit, duration);
		}

		@Override
		protected Boolean doPoll() {
			if (log.isDebugEnabled()) {
				log.debug("Polling writer idle timeout with idleTimeout=" + idleTimeout + " lastWrite=" + lastWrite);
			}
			// return true if we've been idle too long
			return idleTimeout > 0 && lastWrite > 0 && lastWrite + idleTimeout < System.currentTimeMillis();
		}

		@Override
		protected void onPollResult(Boolean result) {
			if (result) {
				try {
					if (log.isDebugEnabled()) {
						log.debug("Idle timeout detected, closing writer and rollong strategies");
					}
					// close the writer and request strategies
					// storage to roll strategies. this is a change
					// to get new unique file, otherwise we'll
					// end up overwriting old one
					close();
					strategiesStorage.rollStrategies();
				}
				catch (IOException e) {
					log.error("error closing", e);
				}
				finally {
					// reset lastWrite so we can wait new write
					lastWrite = Long.MIN_VALUE;
				}
			}
		}

	}

}
