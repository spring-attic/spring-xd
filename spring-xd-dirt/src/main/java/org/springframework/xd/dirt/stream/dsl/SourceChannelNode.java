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
public class SourceChannelNode extends AstNode {

	private final ChannelNode channelNode;

	public SourceChannelNode(ChannelNode channelNode, int endpos) {
		super(channelNode.startpos, endpos);
		this.channelNode = channelNode;
	}

	/** @inheritDoc */
	@Override
	public String stringify(boolean includePositionalInfo) {
		StringBuilder s = new StringBuilder();
		s.append(channelNode.stringify(includePositionalInfo));
		s.append(">");
		return s.toString();
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(channelNode.toString());
		s.append(" > ");
		return s.toString();
	}

	public ChannelNode getChannelNode() {
		return channelNode;
	}

	public void resolve(StreamLookupEnvironment env, String expressionString) {
		channelNode.resolve(env, expressionString);
	}

	public SourceChannelNode copyOf() {
		return new SourceChannelNode(channelNode.copyOf(), endpos);
	}

	public String getChannelName() {
		return channelNode.getChannelName();
	}

	public Object getChannelType() {
		return channelNode.getChannelType();
	}

}
