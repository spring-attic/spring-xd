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

package org.springframework.xd.dirt.stream;

import java.util.Date;

/**
 * A runtime representation of a running stream.
 * 
 * @author Eric Bottard
 */
public class Stream {

	/**
	 * The {@link StreamDefinition} this running instance embodies.
	 */
	private StreamDefinition definition;

	/**
	 * When was this stream started.
	 */
	private Date startedAt = new Date();

	/**
	 * Create a new stream out of the given {@link StreamDefinition}.
	 */
	public Stream(StreamDefinition definition) {
		this.definition = definition;
	}

	public StreamDefinition getStreamDefinition() {
		return definition;
	}

	public Date getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(Date startedAt) {
		this.startedAt = startedAt;
	}

}
