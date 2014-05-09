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

package org.springframework.xd.integration.hadoop.outbound;

import org.springframework.context.SmartLifecycle;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.util.Assert;
import org.springframework.xd.hadoop.fs.HdfsWriter;
import org.springframework.xd.hadoop.fs.HdfsWriterFactory;

/**
 * Spring Integration {@code MessageHandler} handling {@code Message} writing into hdfs using {@code HdfsWriter}.
 * 
 * @author Mark Pollack
 * @author Mark Fisher
 */
public class HdfsWritingMessageHandler extends AbstractMessageHandler implements SmartLifecycle {

	private volatile boolean autoStartup = true;

	private volatile int phase;

	protected final Object lifecycleMonitor = new Object();

	private volatile boolean active;

	private HdfsWriter hdfsWriter;

	private final HdfsWriterFactory hdfsWriterFactory;

	public HdfsWritingMessageHandler(HdfsWriterFactory hdfsWriterFactory) {
		Assert.notNull(hdfsWriterFactory,
				"HdfsWriterFactory must not be null.");
		this.hdfsWriterFactory = hdfsWriterFactory;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		doWrite(message);
	}

	protected void doWrite(Message<?> message) {
		try {
			hdfsWriter.write(message);
		}
		catch (Exception e) {
			throw new MessageHandlingException(message,
					"failed to write Message payload to HDFS", e);
		}
	}


	@Override
	public boolean isRunning() {
		return this.active;
	}

	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			this.hdfsWriter = this.hdfsWriterFactory.createWriter();
		}
	}

	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			this.hdfsWriter.close();
		}
	}

	@Override
	public void stop(Runnable callback) {
		// TODO
		stop();
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}


}
