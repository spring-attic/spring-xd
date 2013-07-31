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

import java.util.List;


/**
 * @author Andy Clement
 */
public class StreamNode extends AstNode {

	private final String streamName;
	private final List<ModuleNode> moduleNodes;
	private SourceChannelNode sourceChannelNode;
	private SinkChannelNode sinkChannelNode;
	
	public StreamNode(String streamName, List<ModuleNode> moduleNodes, SourceChannelNode sourceChannelNode, SinkChannelNode sinkChannelNode) {
		super(moduleNodes.get(0).getStartPos(),moduleNodes.get(moduleNodes.size()-1).getEndPos());
		this.streamName = streamName;
		this.moduleNodes = moduleNodes;
		this.sourceChannelNode = sourceChannelNode;
		this.sinkChannelNode = sinkChannelNode;
	}

	/** @inheritDoc */
	@Override
	public String stringify(boolean includePositionalInfo) {
		StringBuilder s = new StringBuilder();
		s.append("[");
		if (getStreamName()!=null) {
			s.append(getStreamName()).append(" = ");
		}
		if (sourceChannelNode!=null) {
			s.append(sourceChannelNode.stringify(includePositionalInfo));
		}
		for (ModuleNode moduleNode: moduleNodes) {
			s.append(moduleNode.stringify(includePositionalInfo));
		}
		if (sinkChannelNode!=null) {
			s.append(sinkChannelNode.stringify(includePositionalInfo));
		}
		s.append("]");
		return s.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		if (getStreamName()!=null) {
			s.append(getStreamName()).append(" = ");
		}
		if (sourceChannelNode!=null) {
			s.append(sourceChannelNode.toString());
		}
		for (int m=0;m<moduleNodes.size();m++) {
			ModuleNode moduleNode = moduleNodes.get(m);
			s.append(moduleNode.toString());
			if (m+1<moduleNodes.size()) {
				if (moduleNode.isJobStep()) {
					s.append(" & ");
				} else {
					s.append(" | ");
				}
			}
		}
		if (sinkChannelNode!=null) {
			s.append(sinkChannelNode.toString());
		}
		return s.toString();
	}

	public List<ModuleNode> getModuleNodes() {
		return moduleNodes;
	}
	
	public SourceChannelNode getSourceChannelNode() {
		return sourceChannelNode;
	}
	
	public SinkChannelNode getSinkChannelNode() {
		return sinkChannelNode;
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

	public void resolve(StreamLookupEnvironment env) {
		// Module resolution, in the case where substreams are discovered, may introduce
		// source/sink channel so resolve modules first
		resolveModuleNodes(env,moduleNodes);
		if (sourceChannelNode!=null) {
			sourceChannelNode.resolve(env);
		}
	}
	
	/**
	 * Does the lookup of module nodes to see if any previously defined and if so replaces them in this stream
	 */
	public void resolveModuleNodes(StreamLookupEnvironment env, List<ModuleNode> moduleNodes) {
		for (int m=moduleNodes.size()-1;m>=0;m--) {
			ModuleNode moduleNode = moduleNodes.get(m);
			
			StreamNode sn = env.lookupStream(moduleNode.getName());
			if (sn!=null) {
				// Not yet supported - the commented out code below implements specifying
				// source/sink channels in substreams
				if (sn.getSourceChannelNode()!=null) {
					throw new DSLException(null,-1,XDDSLMessages.NO_SOURCE_IN_SUBSTREAM,sn.toString()); 
				}
				if (sn.getSinkChannelNode()!=null) {
					throw new DSLException(null,-1,XDDSLMessages.NO_SINK_IN_SUBSTREAM,sn.toString()); 
				}
//				if (m==0) {
//					// Copy over the source module
//					if (sn.getSourceChannelNode()!=null) {
//						if (getSourceChannelNode()!=null) {
//							throw new InternalParseException(new DSLParseException("",-1,XDDSLMessages.ALREADY_HAS_SOURCE,sn.getSourceChannelNode().stringify(),getSourceChannelNode().stringify())); 
//						}
//					}
//					sourceChannelNode = sn.getSourceChannelNode().copyOf(); 
//				} else if (m==moduleNodes.size()-1) {
//					// Copy over the sink channel
//					if (sn.getSinkChannelNode()!=null) {
//						if (getSinkChannelNode()!=null) {
//							throw new InternalParseException(new DSLParseException("",-1,XDDSLMessages.ALREADY_HAS_SINK,sn.getSourceChannelNode().stringify(),getSourceChannelNode().stringify())); 
//						}
//					}
//					sourceChannelNode = sn.getSourceChannelNode().copyOf();
//				} else {
//					if (sn.getSourceChannelNode()!=null) {
//						throw new InternalParseException(new DSLParseException("",-1,XDDSLMessages.CANNOT_USE_SUBSTREAM_WITH_SOURCE));
//					} else if (sn.getSinkChannelNode()!=null) {
//						throw new InternalParseException(new DSLParseException("",-1,XDDSLMessages.CANNOT_USE_SUBSTREAM_WITH_SINK));
//					}
//				}
				
				// this moduleNode is a reference to an already defined (sub)stream
				// Replace this moduleNode with a copy of the substream
				List<ModuleNode> newNodes = sn.getModuleNodes();
				moduleNodes.remove(m);
				for (int m2 = newNodes.size()-1;m2>=0;m2--) {
					moduleNodes.add(m,newNodes.get(m2).copyOf(moduleNode.getArguments(),newNodes.size()==1));
				}
			}
		}
	}

	public int getIndexOfLabelOrModuleName(String labelOrModuleName) {
		for (int m=0;m<moduleNodes.size();m++) {
			ModuleNode moduleNode = moduleNodes.get(m);
			if (moduleNode.getName().equals(labelOrModuleName)) {
				return m;
			} else {
				for (String labelName: moduleNode.getLabelNames()) {
					if (labelName.equals(labelOrModuleName)) {
						return m;
					}
				}
			}
		}
		return -1;
	}

	public String getStreamData() {
		return toString(); // TODO is toString always ok? currently only used in testing...
	}

}
