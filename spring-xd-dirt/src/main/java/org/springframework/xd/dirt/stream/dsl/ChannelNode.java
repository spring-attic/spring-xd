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

package org.springframework.xd.dirt.stream.dsl;

/**
 * @author Andy Clement
 */
public class ChannelNode extends AstNode {

	private final String streamName;

	private final String channelName;

	public ChannelNode(String streamName, String channelName, int startpos, int endpos) {
		super(startpos, endpos);
		this.streamName = streamName;
		this.channelName = channelName;
	}

	@Override
	public String stringify(boolean includePositionalInfo) {
		StringBuilder s = new StringBuilder();
		s.append("(");
		s.append(":");
		if (streamName != null) {
			s.append(streamName).append(".");
		}
		s.append(channelName);
		if (includePositionalInfo) {
			s.append(":");
			s.append(getStartPos()).append(">").append(getEndPos());
		}
		s.append(")");
		return s.toString();
	}

	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(":");
		if (streamName != null) {
			s.append(streamName).append(".");
		}
		s.append(channelName);
		return s.toString();
	}

	/**
	 * @return stream name which may be null if channel not qualified
	 */
	public String getStreamName() {
		return streamName;
	}

	public String getChannelName() {
		return channelName;
	}

	public ChannelNode copyOf() {
		return new ChannelNode(streamName, channelName, startpos, endpos);
	}

}
