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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Andy Clement
 */
public class StreamNode extends AstNode {

	private final String streamText;

	private final String streamName;

	private final List<ModuleNode> moduleNodes;

	private SourceChannelNode sourceChannelNode;

	private SinkChannelNode sinkChannelNode;

	// List of stream names that have been replaced during resolution of
	// this stream
	private List<String> inlinedStreams = new ArrayList<String>();

	public StreamNode(String streamText, String streamName, List<ModuleNode> moduleNodes,
			SourceChannelNode sourceChannelNode, SinkChannelNode sinkChannelNode) {
		super(moduleNodes.get(0).getStartPos(), moduleNodes.get(moduleNodes.size() - 1).getEndPos());
		this.streamText = streamText;
		this.streamName = streamName;
		this.moduleNodes = moduleNodes;
		this.sourceChannelNode = sourceChannelNode;
		this.sinkChannelNode = sinkChannelNode;
	}

	/** @inheritDoc */
	@Override
	public String stringify(boolean includePositionalInfo) {
		StringBuilder s = new StringBuilder();
		// s.append("Stream[").append(streamText).append("]");
		s.append("[");
		if (getStreamName() != null) {
			s.append(getStreamName()).append(" = ");
		}
		if (sourceChannelNode != null) {
			s.append(sourceChannelNode.stringify(includePositionalInfo));
		}
		for (ModuleNode moduleNode : moduleNodes) {
			s.append(moduleNode.stringify(includePositionalInfo));
		}
		if (sinkChannelNode != null) {
			s.append(sinkChannelNode.stringify(includePositionalInfo));
		}
		s.append("]");
		return s.toString();
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		if (getStreamName() != null) {
			s.append(getStreamName()).append(" = ");
		}
		if (sourceChannelNode != null) {
			s.append(sourceChannelNode.toString());
		}
		for (int m = 0; m < moduleNodes.size(); m++) {
			ModuleNode moduleNode = moduleNodes.get(m);
			s.append(moduleNode.toString());
			if (m + 1 < moduleNodes.size()) {
				s.append(" | ");
			}
		}
		if (sinkChannelNode != null) {
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
	 * Find the first reference to the named module in the stream. If the same module is referred to multiple times the
	 * secondary references cannot be accessed via this method.
	 * 
	 * @return the first occurrence of the named module in the stream
	 */
	public ModuleNode getModule(String moduleName) {
		for (ModuleNode moduleNode : moduleNodes) {
			if (moduleNode.getName().equals(moduleName)) {
				return moduleNode;
			}
		}
		return null;
	}

	public void resolve(StreamLookupEnvironment env) {
		// Module resolution, in the case where substreams are discovered, may introduce
		// source/sink channel so resolve modules first
		resolveModuleNodes(env, moduleNodes);
		if (sourceChannelNode != null) {
			sourceChannelNode.resolve(env);
		}
	}

	/**
	 * Does the lookup of module nodes to see if any previously defined and if so replaces them in this stream.
	 */
	public void resolveModuleNodes(StreamLookupEnvironment env, List<ModuleNode> moduleNodes) {
		for (int m = moduleNodes.size() - 1; m >= 0; m--) {
			ModuleNode moduleNode = moduleNodes.get(m);
			StreamNode sn = env.lookupStream(moduleNode.getName());
			if (sn != null) {
				if (m == 0) {
					if (sn.getSourceChannelNode() != null) {
						if (getSourceChannelNode() != null) {
							throw new StreamDefinitionException(this.streamText, moduleNode.startpos,
									XDDSLMessages.CANNOT_USE_COMPOSEDMODULE_HERE_ALREADY_HAS_SOURCE_CHANNEL,
									moduleNode.getName());
						}
						sourceChannelNode = sn.getSourceChannelNode().copyOf();
					}
				}
				else if (m == moduleNodes.size() - 1) {
					// Copy over the sink channel
					if (sn.getSinkChannelNode() != null) {
						if (getSinkChannelNode() != null) {
							throw new StreamDefinitionException(this.streamText, moduleNode.startpos,
									XDDSLMessages.CANNOT_USE_COMPOSEDMODULE_HERE_ALREADY_HAS_SINK_CHANNEL,
									moduleNode.getName());
						}
						sinkChannelNode = sn.getSinkChannelNode().copyOf();
					}
				}
				else {
					if (sn.getSourceChannelNode() != null) {
						throw new StreamDefinitionException(this.streamText, moduleNode.startpos,
								XDDSLMessages.CANNOT_USE_COMPOSEDMODULE_HERE_AS_IT_DEFINES_SOURCE_CHANNEL,
								moduleNode.getName());
					}
					else if (sn.getSinkChannelNode() != null) {
						throw new StreamDefinitionException(this.streamText, moduleNode.startpos,
								XDDSLMessages.CANNOT_USE_COMPOSEDMODULE_HERE_AS_IT_DEFINES_SINK_CHANNEL,
								moduleNode.getName());
					}
				}

				// this moduleNode is a reference to an already defined stream
				// Replace this moduleNode with a copy of the other stream
				List<ModuleNode> newNodes = sn.getModuleNodes();
				moduleNodes.remove(m);
				for (int m2 = newNodes.size() - 1; m2 >= 0; m2--) {
					moduleNodes.add(m, newNodes.get(m2).copyOf(moduleNode.getArguments(), newNodes.size() == 1));
				}
				inlinedStreams.add(moduleNode.getName());
			}
		}
		// Check no duplicate labels across the stream
		Map<String, ModuleNode> labeledModules = new HashMap<String, ModuleNode>();
		for (int m = 0, max = moduleNodes.size(); m < max; m++) {
			ModuleNode moduleNode = moduleNodes.get(m);
			if (moduleNode.getLabelNames().size() != 0) {
				List<String> labels = moduleNode.getLabelNames();
				for (String label : labels) {
					ModuleNode existingLabeledModule = labeledModules.get(label);
					if (existingLabeledModule != null) {
						// ERROR
						throw new StreamDefinitionException(this.streamText, existingLabeledModule.startpos,
								XDDSLMessages.DUPLICATE_LABEL, label, existingLabeledModule.getName(),
								moduleNode.getName());
					}
					labeledModules.put(label, moduleNode);
				}
			}
		}
	}

	public int getIndexOfLabelOrModuleName(String labelOrModuleName) {
		for (int m = 0; m < moduleNodes.size(); m++) {
			ModuleNode moduleNode = moduleNodes.get(m);
			if (moduleNode.getName().equals(labelOrModuleName)) {
				return m;
			}
			else {
				for (String labelName : moduleNode.getLabelNames()) {
					if (labelName.equals(labelOrModuleName)) {
						return m;
					}
				}
			}
		}
		return -1;
	}

	public boolean labelOrModuleNameOccursMultipleTimesInStream(String labelOrModuleName) {
		int foundModule = -1;
		for (int m = 0; m < moduleNodes.size(); m++) {
			ModuleNode moduleNode = moduleNodes.get(m);
			if (moduleNode.getName().equals(labelOrModuleName)) {
				if (foundModule != -1) {
					return true;
				}
				foundModule = m;
			}
			else {
				for (String labelName : moduleNode.getLabelNames()) {
					if (labelName.equals(labelOrModuleName)) {
						if (foundModule != -1) {
							return true;
						}
						foundModule = m;
					}
				}
			}
		}
		return false;
	}

	public String getStreamData() {
		return toString(); // TODO is toString always ok? currently only used in testing...
	}

	public String getStreamText() {
		return this.streamText;
	}

	public String getName() {
		return this.streamName;
	}

	public List<String> getInlinedStream() {
		return this.inlinedStreams;
	}

}
