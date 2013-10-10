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
public class SinkChannelNode extends AstNode {

	private final ChannelNode channelNode;

	public SinkChannelNode(ChannelNode channelNode, int startpos) {
		super(startpos, channelNode.endpos);
		this.channelNode = channelNode;
	}

	/** @inheritDoc */
	@Override
	public String stringify(boolean includePositionalInfo) {
		StringBuilder s = new StringBuilder();
		s.append(">");
		s.append(channelNode.stringify(includePositionalInfo));
		return s.toString();
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(" > ");
		s.append(channelNode.toString());
		return s.toString();
	}

	public ChannelNode getChannelNode() {
		return channelNode;
	}

	public String getChannelName() {
		return channelNode.getChannelName();
	}

	public SinkChannelNode copyOf() {
		return new SinkChannelNode(channelNode.copyOf(), startpos);
	}

}
