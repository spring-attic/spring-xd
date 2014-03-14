/*
 * Copyright 2013-2014 the original author or authors.
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

/**
 * A runtime representation of a running stream.
 * 
 * @author Eric Bottard
 * @author Gunnar Hillert
 * 
 * @since 1.0
 */
public class Stream extends BaseInstance<StreamDefinition> implements Comparable<Stream> {

	/**
	 * Create a new stream out of the given {@link StreamDefinition}.
	 */
	public Stream(StreamDefinition definition) {
		super(definition);
	}

	@Override
	public int compareTo(Stream other) {
		int diff = this.getDefinition().getName().compareTo(other.getDefinition().getName());
		return (diff != 0) ? diff : this.getStartedAt().compareTo(other.getStartedAt());
	}
}
