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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Andy Clement
 */
public class ModuleNode extends AstNode {

	private static final ArgumentNode[] NO_ARGUMENTS = new ArgumentNode[0];

	private List<LabelNode> labels;

	private final String moduleName;

	private ArgumentNode[] arguments;

	public ModuleNode(List<LabelNode> labels, String moduleName, int startpos, int endpos, ArgumentNode[] arguments) {
		super(startpos, endpos);
		this.labels = labels;
		this.moduleName = moduleName;
		if (arguments != null) {
			this.arguments = Arrays.copyOf(arguments, arguments.length);
			// adjust end pos for module node to end of final argument
			this.endpos = this.arguments[this.arguments.length - 1].endpos;
		}
		else {
			this.arguments = NO_ARGUMENTS;
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		if (labels != null) {
			for (LabelNode label : labels) {
				s.append(label.toString());
				s.append(" ");
			}
		}
		s.append(moduleName);
		if (arguments != null) {
			for (int a = 0; a < arguments.length; a++) {
				s.append(" --").append(arguments[a].getName()).append("=").append(arguments[a].getValue());
			}
		}
		return s.toString();
	}

	@Override
	public String stringify(boolean includePositionalInfo) {
		StringBuilder s = new StringBuilder();
		s.append("(");
		if (labels != null) {
			for (LabelNode label : labels) {
				s.append(label.stringify(includePositionalInfo));
				s.append(" ");
			}
		}
		s.append("ModuleNode:").append(moduleName);
		if (arguments != null) {
			for (int a = 0; a < arguments.length; a++) {
				s.append(" --").append(arguments[a].getName()).append("=").append(arguments[a].getValue());
			}
		}
		if (includePositionalInfo) {
			s.append(":");
			s.append(getStartPos()).append(">").append(getEndPos());
		}
		s.append(")");
		return s.toString();
	}

	public String getName() {
		return moduleName;
	}

	public ArgumentNode[] getArguments() {
		return arguments;
	}

	public boolean hasArguments() {
		return arguments != null;
	}

	public List<String> getLabelNames() {
		if (labels == null) {
			return Collections.emptyList();
		}
		List<String> labelNames = new ArrayList<String>();
		for (LabelNode label : labels) {
			labelNames.add(label.getLabelName());
		}
		return labelNames;
	}

	/**
	 * @return Retrieve the module arguments as a simple {@link java.util.Properties} object.
	 */
	public Properties getArgumentsAsProperties() {
		Properties props = new Properties();
		if (arguments != null) {
			for (ArgumentNode argumentNode : arguments) {
				props.put(argumentNode.getName(), argumentNode.getValue());
			}
		}
		return props;
	}

	/**
	 * Whilst working through arguments when creating a copy of the module, instances of this class tag whether an
	 * argument has been used to satisfy a variable in a parameterized stream (e.g. ${NAME}).
	 */
	static class ConsumableArgumentNode {

		private boolean consumed;

		ArgumentNode argumentNode;

		ConsumableArgumentNode(ArgumentNode argumentNode) {
			this.consumed = false;
			this.argumentNode = argumentNode;
		}

		public void setConsumed(boolean consumed) {
			this.consumed = consumed;
		}

		public boolean isConsumed() {
			return this.consumed;
		}
	}

	/**
	 * Construct a copy of the module node but the supplied replacement arguments can adjust the argument set that the
	 * resultant copy will have, in three ways: - they can be used to fill in variables in parameters - they can
	 * override existing parameters with the same name - they can behave as additional parameters
	 */
	public ModuleNode copyOf(ArgumentNode[] arguments, boolean argumentOverriding) {
		Map<String, ConsumableArgumentNode> extraArgumentsMap = new LinkedHashMap<String, ConsumableArgumentNode>();
		if (arguments != null) {
			for (ArgumentNode argument : arguments) {
				extraArgumentsMap.put(argument.getName(), new ConsumableArgumentNode(argument));
			}
		}

		Map<String, ArgumentNode> newModuleArguments = new LinkedHashMap<String, ArgumentNode>();

		// Variable replacement first
		if (this.arguments != null) {
			for (ArgumentNode existingArgument : this.arguments) {
				ArgumentNode arg = existingArgument.withReplacedVariables(extraArgumentsMap);
				newModuleArguments.put(arg.getName(), arg);
			}
		}

		if (argumentOverriding) {
			for (ConsumableArgumentNode can : extraArgumentsMap.values()) {
				if (!can.isConsumed()) {
					newModuleArguments.put(can.argumentNode.getName(), can.argumentNode);
				}
			}
		}
		ArgumentNode[] newModuleArgumentsArray = null;
		if (newModuleArguments.size() != 0) {
			newModuleArgumentsArray =
					newModuleArguments.values().toArray(new ArgumentNode[newModuleArguments.values().size()]);
		}
		return new ModuleNode(this.labels, this.moduleName, this.startpos, this.endpos, newModuleArgumentsArray);
	}

}
