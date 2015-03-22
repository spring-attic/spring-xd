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

package org.springframework.xd.dirt.test.sink;

import org.springframework.messaging.Message;
import org.springframework.xd.dirt.test.NamedChannelModule;


/**
 * A Sink bound to a named channel.
 * 
 * @author David Turanski
 */
public interface NamedChannelSink extends NamedChannelModule {

	/**
	 * Receive the first available message from this sink.
	 * <p>
	 * If the sink contains no messages, this method will block.
	 *
	 * @return the first available message or <code>null</code> if the receiving
	 * thread is interrupted.
	 */
	Message<?> receive();

	/**
	 * Receive the first available message from this sink.
	 * <p>
	 * If the channel contains no messages, this method will block until the allotted timeout elapses.
	 * If the specified timeout is 0, the method will return immediately.
	 * If less than zero, it will block indefinitely (see {@link #receive()}).
	 *
	 * @param timeout the timeout in milliseconds
	 *
	 * @return the first available message or <code>null</code> if no message is available within the
	 * allotted time or the receiving thread is interrupted.
	 */
	Message<?> receive(int timeout);

	/**
	 * Receive the first available payload from this sink.
	 * <p>
	 * If the sink contains no messages, this method will block.
	 *
	 * @return the first available payload or <code>null</code> if the receiving
	 * thread is interrupted.
	 */
	Object receivePayload();

	/**
	 * Receive the first available payload from this sink.
	 * <p>
	 * If the channel contains no messages, this method will block until the allotted timeout elapses.
	 * If the specified timeout is 0, the method will return immediately.
	 * If less than zero, it will block indefinitely (see {@link #receivePayload()}).
	 *
	 * @param timeout the timeout in milliseconds
	 *
	 * @return the first available message or <code>null</code> if no message is available within the
	 * allotted time or the receiving thread is interrupted.
	 */
	Object receivePayload(int timeout);

}
