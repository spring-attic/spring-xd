/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.stream.dsl;

import static org.springframework.xd.dirt.stream.dsl.XDDSLMessages.UNRECOGNIZED_MODULE_REFERENCE;
import static org.springframework.xd.dirt.stream.dsl.XDDSLMessages.UNRECOGNIZED_STREAM_REFERENCE;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andy Clement
 * @author David Turanski
 */
public class ChannelNode extends AstNode {

	private ChannelType channelType;

	private List<String> nameComponents;

	private List<String> indexingElements;

	public ChannelNode(ChannelType channelType, int startpos, int endpos, List<String> nameElements,
			List<String> indexingElements) {
		super(startpos, endpos);
		this.channelType = channelType;
		this.nameComponents = nameElements;
		this.indexingElements = indexingElements;
	}

	@Override
	public String stringify(boolean includePositionalInfo) {
		StringBuilder s = new StringBuilder();
		s.append("(");
		produceStringRepresentation(s);
		if (includePositionalInfo) {
			s.append(":");
			s.append(getStartPos()).append(">").append(getEndPos());
		}
		s.append(")");
		return s.toString();
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		produceStringRepresentation(s);
		return s.toString();
	}

	private void produceStringRepresentation(StringBuilder s) {
		int t = 0;
		if (channelType.isTap()) {
			s.append(channelType.getStringRepresentation());
		}
		if (nameComponents.size() > 0 && channelType.isTap() &&
				nameComponents.get(0).equalsIgnoreCase(channelType.tapSource().name())) {
			t = 1;
		}
		for (int max = nameComponents.size(); t < max; t++) {
			s.append(nameComponents.get(t));
			if (t < nameComponents.size() - 1) {
				s.append(":");
			}
		}
		if (indexingElements.size() != 0) {
			for (int t2 = 0, max = indexingElements.size(); t2 < max; t2++) {
				s.append(".");
				s.append(indexingElements.get(t2));
			}
		}
	}

	String getChannelName() {
		StringBuilder s = new StringBuilder();
		if (channelType.isTap()) {
			s.append("tap:");
		}
		s.append(getNameComponents());
		s.append(getIndexingComponents());
		return s.toString();
	}

	private int getLengthOfPrefixPlusNameComponents() {
		int length = 0;
		if (channelType.isTap()) {
			length += 4;
		}
		for (int i = 0; i < nameComponents.size(); i++) {
			if (i > 0) {
				length++;
			}
			length += nameComponents.get(i).length();
		}
		return length;
	}

	private String getNameComponents() {
		StringBuilder s = new StringBuilder();
		for (int t = 0, max = nameComponents.size(); t < max; t++) {
			if (t > 0) {
				s.append(":");
			}
			s.append(nameComponents.get(t));
		}
		return s.toString();
	}

	private String getIndexingComponents() {
		StringBuilder s = new StringBuilder();
		for (int t = 0, max = indexingElements.size(); t < max; t++) {
			s.append(".");
			s.append(indexingElements.get(t));
		}
		return s.toString();
	}

	ChannelType getChannelType() {
		return this.channelType;
	}

	public ChannelNode copyOf() {
		// TODO not a deep copy, is that ok?
		return new ChannelNode(this.channelType, startpos, endpos, nameComponents, indexingElements);
	}

	public void resolve(StreamLookupEnvironment env, String expressionString) {
		if (channelType == ChannelType.TAP_STREAM) {
			String streamName = nameComponents.get(1);
			StreamNode sn = env.lookupStream(streamName);
			if (sn == null) {
				int offset = this.startpos + channelType.getStringRepresentation().length();
				throw new StreamDefinitionException(expressionString, offset, UNRECOGNIZED_STREAM_REFERENCE, streamName);
			}

			if (indexingElements.isEmpty()) {
				// Point to the first element of the stream
				indexingElements = new ArrayList<String>();
				indexingElements.add(sn.getModuleNodes().get(0).getName() + ".0");
			}
			else {
				// Easter Egg: can use index of module in a stream when tapping.
				try {
					int index = Integer.parseInt(indexingElements.get(0));
					indexingElements.remove(0);
					indexingElements.add(0, sn.getModuleNodes().get(index).getName() + "." + index);
				}
				catch (NumberFormatException nfe) {
					// this is ok, probably wasn't a number
					String indexString = toString(indexingElements);
					int index = sn.getIndexOfLabel(indexString);
					if (index != -1) {
						indexingElements.clear();
						indexingElements.add(0, sn.getModuleNodes().get(index).getName() + "." + index);
					}
					else {
						int offset = this.startpos + getLengthOfPrefixPlusNameComponents() + 1;
						throw new StreamDefinitionException(expressionString, offset,
								UNRECOGNIZED_MODULE_REFERENCE, indexString);
					}
				}
			}
		}
	}

	private String toString(List<String> elements) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < elements.size(); i++) {
			if (i > 0) {
				s.append('.');
			}
			s.append(elements.get(i));
		}
		return s.toString();
	}
}
