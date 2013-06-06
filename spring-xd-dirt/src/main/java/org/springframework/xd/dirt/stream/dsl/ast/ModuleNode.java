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

import java.util.Properties;

/**
 * @author Andy Clement
 */
public class ModuleNode extends AstNode {

	private String moduleName;
	private ArgumentNode[] arguments;
	
	public ModuleNode(String moduleName, int startpos, int endpos, ArgumentNode[] arguments) {
		super(startpos,endpos);
		this.moduleName = moduleName;
		if (arguments != null) {
			this.arguments = arguments;
			// adjust end pos for module node to end of final argument
			this.endpos = this.arguments[this.arguments.length-1].endpos;
		}
	}
	
	@Override
	public String stringify() {
		StringBuilder s = new StringBuilder();
		s.append("(").append("ModuleNode:").append(moduleName);
		if (arguments != null) {
			for (int a=0;a<arguments.length;a++) {
				s.append(" --").append(arguments[a].getName()).append("=").append(arguments[a].getValue());
			}
		}
		s.append(":");
		s.append(getStartPos()).append(">").append(getEndPos());
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
		return arguments!=null;
	}

	/**
	 * @return Retrieve the module arguments as a simple {@link java.util.Properties} object.
	 */
	public Properties getArgumentsAsProperties() {
		Properties props = new Properties();
		if (arguments!=null) {
			for (ArgumentNode argumentNode: arguments) {
				props.put(argumentNode.getName(), argumentNode.getValue());
			}
		}
		return props;
	}

}
