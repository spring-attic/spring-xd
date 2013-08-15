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
public class ModuleReferenceNode extends AstNode {

	private final String streamName;
	private final String labelOrModuleName;
	
	private String resolvedChannel;

	public ModuleReferenceNode(String streamName, String moduleName, int startpos, int endpos) {
		super(startpos,endpos);
		this.streamName = streamName;
		this.labelOrModuleName = moduleName;
	}

	@Override
	public String stringify(boolean includePositionalInfo) {
		StringBuilder s = new StringBuilder();
		s.append("(");
		if (streamName != null) {
			s.append(streamName).append(".");
		}
		s.append(labelOrModuleName);
		if (resolvedChannel!=null) {
			s.append("[[channel:").append(resolvedChannel).append("]]");
		}
		if (includePositionalInfo) {
			s.append(":");
			s.append(getStartPos()).append(">").append(getEndPos());
		}
		s.append(")");
		return s.toString();
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		if (streamName != null) {
			s.append(streamName).append(".");
		}
		s.append(labelOrModuleName);
		return s.toString();
	}

	/**
	 * @return stream name which may be null if channel not qualified
	 */
	public String getStreamName() {
		return streamName;
	}
	
	public String getModuleName() {
		return labelOrModuleName;
	}

	public void resolve(StreamLookupEnvironment env) {
		resolvedChannel = env.lookupChannelForLabelOrModule(streamName, labelOrModuleName);
		if (streamName==null && resolvedChannel == null) {
			// it is possible the singular name in labelOrModuleName is actually
			// a stream reference
			StreamNode sn = env.lookupStream(labelOrModuleName);
			if (sn!=null) {
				resolvedChannel = sn.getStreamName()+".0";
			}
		}
	}

	public ModuleReferenceNode copyOf() {
		ModuleReferenceNode moduleReferenceNode =
			new ModuleReferenceNode(streamName, labelOrModuleName, startpos, endpos);
		moduleReferenceNode.resolvedChannel = resolvedChannel;
		return moduleReferenceNode;
	}
	
	public String getChannelName() {
		return resolvedChannel;
	}

}
