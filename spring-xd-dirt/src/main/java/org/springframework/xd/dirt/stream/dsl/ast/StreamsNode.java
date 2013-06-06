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

// TODO [Andy] not sure this ast node is necessary if we never parse multiple stream definitions in one go
/**
 * @author Andy Clement
 */
public class StreamsNode extends AstNode {
	
	private String streamsText;
	private List<StreamNode> streamNodes;

	public StreamsNode(String streamsText, List<StreamNode> streamNodes) {
		super(streamNodes.get(0).getStartPos(),streamNodes.get(streamNodes.size()-1).getEndPos());
		this.streamsText = streamsText;
		this.streamNodes = streamNodes;
	}
	
	@Override
	public String stringify() {
		StringBuilder s = new StringBuilder();
		s.append("Streams[").append(streamsText).append("]");
		if (streamNodes.size()>1) {
			s.append("\n");
		}
		for (int str=0;str<streamNodes.size();str++) {
			if (str>0) {
				s.append("\n");
			}			
			s.append(streamNodes.get(str).stringify());
		}
		return s.toString();
	}

	public List<StreamNode> getStreamNodes() {
		return streamNodes;
	}
	
	public String getStreamsText() {
		return streamsText;
	}

	public List<ModuleNode> getModules() {
		return streamNodes.get(0).getModuleNodes();
	}

	public List<ModuleNode> getModuleNodes() {
		return streamNodes.get(0).getModuleNodes();
	}

	public ModuleNode getModule(String moduleName) {
		for (StreamNode streamNode: streamNodes) {
			ModuleNode moduleNode = streamNode.getModule(moduleName);
			if (moduleNode!=null) {
				return moduleNode;
			}
		}
		return null;
	}

	public List<StreamNode> getStreams() {
		return streamNodes;
	}

}
