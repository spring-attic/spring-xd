package org.springframework.xd.extension.process;/*
 *
 *  * Copyright 2014 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.integration.ip.tcp.serializer.AbstractByteArraySerializer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Creates a process to run a shell command and communicate with it using String payloads over stdin and stdout
 *
 * @author David Turanski
 * @since 1.0
 */
public class ShellCommandProcessor implements Lifecycle {
	private final AtomicBoolean running = new AtomicBoolean();
	private final ProcessBuilder processBuilder;
	private Process process;
	private InputStream stdout;
	private OutputStream stdin;
	private final AbstractByteArraySerializer serializer;
	private static Log log = LogFactory.getLog(ShellCommandProcessor.class);

	/**
	 * Creates a process to invoke a shell command to send and receive messages from the processes using the process's stdin and stdout. Stderr is
	 * redirected to stdout.
	 *
	 * @param serializer an {@link org.springframework.integration.ip.tcp.serializer.AbstractByteArraySerializer} to delimit messages
	 * @param command the shell command with command line arguments as separate strings
	 */
	public ShellCommandProcessor(AbstractByteArraySerializer serializer, String... command) {
		Assert.isTrue(command.length > 0,"A valid shell command is required");
		Assert.notNull(command,"A valid shell command is required");
		Assert.notNull(serializer,"'serializer' cannot be null");
		this.serializer = serializer;
	   	processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true);
	}

	/**
	 *
	 * @param serializer an {@link org.springframework.integration.ip.tcp.serializer.AbstractByteArraySerializer} to delimit messages
	 * @param command command as space delimited string
	 */
	public ShellCommandProcessor(AbstractByteArraySerializer serializer, String command) {
		this(serializer, StringUtils.delimitedListToStringArray(command," "));
	}

	/**
	 * Start the process
	 */
	@Override
	public void start() {
		if (!isRunning()) {
			try {
				if (log.isDebugEnabled()) {
					log.debug("starting process");
				}
				process = processBuilder.start();
				stdout = process.getInputStream();
				stdin = process.getOutputStream();
				running.set(true);
				if (log.isDebugEnabled()) {
					log.debug("process started");
				}
			}
			catch (IOException e) {
				log.error(e.getMessage(),e);
				throw new RuntimeException(e.getMessage(),e);
			}
		}
	}

	/**
	 * Receive data from the process
	 * @return any available data from stdout
	 */
	public String receive() {
		Assert.isTrue(isRunning(),"Shell process is not started");
		String data;
		try {
			byte[] buffer = serializer.deserialize(stdout);
			data = new String(buffer,"UTF-8");
		}
		catch (IOException e) {
			throw new RuntimeException(e.getMessage(),e);
		}
		return data == null? null : data.trim();
	}

	/**
	 * Send data as a String to stdin
	 * @param data the data
	 */
	public void send(String data) {
		try {
			serializer.serialize(data.getBytes("UTF-8"), stdin);
		} catch (IOException e) {
			log.error(e.getMessage(),e);
			throw new RuntimeException(e.getMessage(),e);
		}
	}

	/**
	 * Send and receive data in request/response fashion.
	 * @param data the input
	 * @return the output
	 */
	public String sendAndReceive(String data) {
		Assert.isTrue(isRunning(),"Shell process is not started");
		send(data);
		return receive();
	}

	/**
	 * Stop the process and close streams
	 */
	@Override
	public void stop() {
	   if (isRunning()) {
		   process.destroy();
		   try {
			   if (stdout !=null) {
				   stdout.close();
			   }
			   if (stdin !=null) {
				   stdin.close();
			   }
		   }
		   catch (IOException e) {
			   throw new RuntimeException(e.getMessage(),e);
		   }
		   finally {
			   running.set(false);
		   }
	   }
	}

	@Override
	public boolean isRunning() {
		return running.get();
	}

}
