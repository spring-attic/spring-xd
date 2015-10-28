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

package org.springframework.xd.extension.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.ip.tcp.serializer.AbstractByteArraySerializer;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Creates a process to run a shell command and communicate with it using String payloads over stdin and stdout.
 *
 * @author David Turanski
 * @author Gary Russell
 */
public class ShellCommandProcessor implements Lifecycle, InitializingBean {

	private volatile boolean running = false;

	private final ProcessBuilder processBuilder;

	private volatile Process process;

	private volatile InputStream stdout;

	private volatile OutputStream stdin;

	private boolean redirectErrorStream;

	private final Map<String, String> environment = new ConcurrentHashMap<>();

	private volatile String workingDirectory;

	private volatile String charset = "UTF-8";

	private final AbstractByteArraySerializer serializer;

	private final static Logger log = LoggerFactory.getLogger(ShellCommandProcessor.class);

	private final TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	private final String command;

	private final Object lifecycleLock = new Object();


	/**
	 * Creates a process to invoke a shell command to send and receive messages from the processes using the process's stdin and stdout.
	 *
	 * @param serializer an {@link org.springframework.integration.ip.tcp.serializer.AbstractByteArraySerializer} to delimit messages
	 * @param command the shell command with command line arguments as separate strings
	 */
	public ShellCommandProcessor(AbstractByteArraySerializer serializer, String command) {
		Assert.hasLength(command, "A shell command is required");
		Assert.notNull(serializer, "'serializer' cannot be null");
		this.command = command;
		ShellWordsParser shellWordsParser = new ShellWordsParser();
		List<String> commandPlusArgs = shellWordsParser.parse(command);
		Assert.notEmpty(commandPlusArgs, "The shell command is invalid: '" + command + "'");
		this.serializer = serializer;
		processBuilder = new ProcessBuilder(commandPlusArgs);
	}

	/**
	 * Start the process.
	 */
	@Override
	public void start() {
		synchronized (lifecycleLock) {
			if (!isRunning()) {
				if (log.isDebugEnabled()) {
					log.debug("starting process. Command = [" + command + "]");
				}

				try {
					process = processBuilder.start();
				}
				catch (IOException e) {
					log.error(e.getMessage(), e);
					throw new RuntimeException(e.getMessage(), e);
				}

				if (!processBuilder.redirectErrorStream()) {
					monitorErrorStream();
				}
				monitorProcess();

				stdout = process.getInputStream();
				stdin = process.getOutputStream();

				running = true;
				if (log.isDebugEnabled()) {
					log.debug("process started. Command = [" + command + "]");
				}
			}
		}
	}

	/**
	 * Receive data from the process.
	 * @return any available data from stdout
	 */
	public synchronized String receive() {
		Assert.isTrue(isRunning(), "Shell process is not started.");
		String data;
		try {
			byte[] buffer = this.serializer.deserialize(this.stdout);
			data = new String(buffer, this.charset);
		}
		catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		return data.trim();
	}

	/**
	 * Send data as a String to stdin.
	 * @param data the data
	 */
	public synchronized void send(String data) {
		Assert.isTrue(isRunning(), "Shell process is not started.");
		try {
			this.serializer.serialize(data.getBytes(this.charset), this.stdin);
			this.stdin.flush();
		}
		catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Send and receive data in request/response fashion.
	 * @param data the input
	 * @return the output
	 */
	public synchronized String sendAndReceive(String data) {
		Assert.isTrue(isRunning(), "Shell process is not started");
		send(data);
		return receive();
	}

	/**
	 * Stop the process and close streams.
	 */
	@Override
	public void stop() {
		synchronized (lifecycleLock) {
			if (isRunning()) {
				process.destroy();
				running = false;
			}
		}
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	/**
	 * Set to true to redirect stderr to stdout.
	 * @param redirectErrorStream
	 */
	public void setRedirectErrorStream(boolean redirectErrorStream) {
		this.redirectErrorStream = redirectErrorStream;
	}

	/**
	 * A map containing environment variables to add to the process environment.
	 * @param environment
	 */
	public void setEnvironment(Map<String, String> environment) {
		this.environment.putAll(environment);
	}

	/**
	 * Set the process working directory
	 * @param workingDirectory the file path
	 */
	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	/**
	 * Set the charset name for String encoding. Default is UTF-8
	 * @param charset the charset name
	 */
	public void setCharset(String charset) {
		this.charset = charset;//NOSONAR
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		processBuilder.redirectErrorStream(redirectErrorStream);

		if (StringUtils.hasLength(workingDirectory)) {
			processBuilder.directory(new File(workingDirectory));
		}
		if (!CollectionUtils.isEmpty(environment)) {
			processBuilder.environment().putAll(environment);
		}
	}

	/**
	 * Runs a thread that waits for the Process result.
	 */
	private void monitorProcess() {
		taskExecutor.execute(new Runnable() {

			@Override
			public void run() {
				Process process = ShellCommandProcessor.this.process;
				if (process == null) {
					if (log.isDebugEnabled()) {
						log.debug("Process destroyed before starting process monitor");
					}
					return;
				}

				int result;
				try {
					if (log.isDebugEnabled()) {
						log.debug("Monitoring process '" + command + "'");
					}
					result = process.waitFor();
					if (log.isInfoEnabled()) {
						log.info("Process '" + command + "' terminated with value " + result);
					}
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					log.error("Interrupted - stopping adapter", e);
					stop();
				}
				finally {
					process.destroy();
				}
			}
		});
	}

	/**
	 * Runs a thread that reads stderr
	 */
	private void monitorErrorStream() {
		Process process = this.process;
		if (process == null) {
			if (log.isDebugEnabled()) {
				log.debug("Process destroyed before starting stderr reader");
			}
			return;
		}
		final BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		taskExecutor.execute(new Runnable() {

			@Override
			public void run() {
				String statusMessage;
				if (log.isDebugEnabled()) {
					log.debug("Reading stderr");
				}
				try {
					while ((statusMessage = errorReader.readLine()) != null) {
						log.error(statusMessage);
					}
				}
				catch (IOException e) {
					if (log.isDebugEnabled()) {
						log.debug("Exception on process error reader", e);
					}
				}
				finally {
					try {
						errorReader.close();
					}
					catch (IOException e) {
						if (log.isDebugEnabled()) {
							log.debug("Exception while closing stderr", e);
						}
					}
				}
			}
		});
	}

}
