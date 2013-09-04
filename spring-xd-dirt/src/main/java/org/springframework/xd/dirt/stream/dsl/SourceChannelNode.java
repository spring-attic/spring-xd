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

	private final ModuleReferenceNode moduleReferenceNode;

	private final boolean isTap;

	public SourceChannelNode(ChannelNode channelNode, int endpos, boolean isTap) {
		super(channelNode.startpos, endpos);
		this.channelNode = channelNode;
		this.moduleReferenceNode = null;
		this.isTap = isTap;
	}

	public SourceChannelNode(ModuleReferenceNode moduleReferenceNode, int endpos, boolean isTap) {
		super(moduleReferenceNode.startpos, endpos);
		this.moduleReferenceNode = moduleReferenceNode;
		this.channelNode = null;
		this.isTap = isTap;
		// assert isTap==true
	}

	/** @inheritDoc */
	@Override
	public String stringify(boolean includePositionalInfo) {
		StringBuilder s = new StringBuilder();
		if (isTap) {
			s.append("tap ");
		}
		if (channelNode != null) {
			s.append(channelNode.stringify(includePositionalInfo));
		}
		else {
			s.append(moduleReferenceNode.stringify(includePositionalInfo));
		}
		s.append(">");
		return s.toString();
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		if (isTap) {
			s.append("tap ");
		}
		if (channelNode != null) {
			s.append(channelNode.toString());
		}
		else {
			s.append(moduleReferenceNode.toString());
		}
		s.append(" > ");
		return s.toString();
	}

	public ChannelNode getChannelNode() {
		return channelNode;
	}

	public ModuleReferenceNode getModuleReferenceNode() {
		return moduleReferenceNode;
	}

	public boolean isTap() {
		return isTap;
	}

	public void resolve(StreamLookupEnvironment env) {
		if (channelNode == null) {
			// It is a module reference, needs to be resolved
			moduleReferenceNode.resolve(env);
		}
		else {
			channelNode.resolve(env);
		}
	}

	public SourceChannelNode copyOf() {
		if (channelNode != null) {
			return new SourceChannelNode(channelNode.copyOf(), endpos, isTap);
		}
		else {
			return new SourceChannelNode(moduleReferenceNode.copyOf(), endpos, isTap);
		}
	}

	public String getChannelName() {
		if (channelNode != null) {
			return channelNode.getChannelName();
		}
		else {
			return moduleReferenceNode.getChannelName();
		}
	}

}
