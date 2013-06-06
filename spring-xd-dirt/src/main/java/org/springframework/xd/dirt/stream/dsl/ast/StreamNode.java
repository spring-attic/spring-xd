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
package org.springframework.xd.dirt.stream.dsl.ast;

import java.util.List;

/**
 * @author Andy Clement
 */
public class StreamNode extends AstNode {

	private final String streamName;
	private final List<ModuleNode> moduleNodes;

	public StreamNode(String streamName, List<ModuleNode> moduleNodes) {
		super(moduleNodes.get(0).getStartPos(),moduleNodes.get(moduleNodes.size()-1).getEndPos());
		this.streamName = streamName;
		this.moduleNodes = moduleNodes;
	}

	/** @inheritDoc */
	@Override
	public String stringify() {
		StringBuilder s = new StringBuilder();
		s.append("[");
		if (getStreamName()!=null) {
			s.append(getStreamName()).append(" = ");
		}
		for (ModuleNode moduleNode: moduleNodes) {
			s.append(moduleNode.stringify());
		}
		s.append("]");
		return s.toString();
	}

	public List<ModuleNode> getModuleNodes() {
		return moduleNodes;
	}

	public String getStreamName() {
		// TODO [Andy] if null, could compute it using the module2module2module... algorithm
		return streamName;
	}

	/**
	 * Find the first reference to the named module in the stream. If the same module is
	 * referred to multiple times the secondary references cannot
	 * be accessed via this method.
	 *
	 * @return the first occurrence of the named module in the stream
	 */
	public ModuleNode getModule(String moduleName) {
		for (ModuleNode moduleNode: moduleNodes) {
			if (moduleNode.getName().equals(moduleName)) {
				return moduleNode;
			}
		}
		return null;
	}

}
