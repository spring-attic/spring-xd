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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.xd.dirt.stream.dsl.DSLException;
import org.springframework.xd.dirt.stream.dsl.XDDSLMessages;
import org.springframework.xd.dirt.stream.dsl.ast.ModuleNode.ConsumedArgumentNode;

/**
 * Represents an argument like "--name=value".
 * 
 * @author Andy Clement
 */
public class ArgumentNode extends AstNode {

	private final String name;
	private final String value;

	public ArgumentNode(String name, String value, int startpos, int endpos) {
		super(startpos, endpos);
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String stringify(boolean includePositionalInfo) {
		StringBuilder s = new StringBuilder();
		s.append("--").append(name).append("=").append(value);
		return s.toString();
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("--").append(name).append("=").append(value);
		return s.toString();
	}

	public ArgumentNode withReplacedVariables(Map<String, ConsumedArgumentNode> argumentMap) {
		String argumentValue = value;
		List<Variable> variables = getVariablesInValue();
		for (int v=variables.size()-1;v>=0;v--) {
			Variable variable = variables.get(v);
			ConsumedArgumentNode replacement = argumentMap.get(variable.name);
			String newValue = null;
			if (replacement!=null) {
				replacement.consumed=true;
				newValue = replacement.argumentNode.getValue();
			} else {
				newValue = variable.defaultValue;
			}
			if (newValue==null) {
				throw new DSLException(null, -1, XDDSLMessages.MISSING_VALUE_FOR_VARIABLE, 
						variable.name);
			}
			StringBuilder s = new StringBuilder();
			s.append(argumentValue.substring(0,variable.start));
			s.append(newValue);
			s.append(argumentValue.substring(variable.end+1));
			argumentValue = s.toString();
		}
		return new ArgumentNode(name,argumentValue,startpos,endpos);
	}
	
	static class Variable {
		public Variable(String name2, String defaultValue2, int startIndex,
				int pos) {
			this.name = name2;
			this.defaultValue = defaultValue2;
			this.start = startIndex;
			this.end = pos;
		}
		String name;
		String defaultValue;
		int start;
		int end;		
	}
	
	private Variable locateVariable(int startIndex) {
		int pos = startIndex+2;
		int defaultValueStart = -1;
		boolean variableEnded = false;
		while (!variableEnded && pos<value.length()) {
			char ch = value.charAt(pos++);
			if (ch=='}') {
				variableEnded = true;
			} else if (ch==':') {
				defaultValueStart = pos;
			}
		}
		if (!variableEnded) {
			throw new DSLException(null,-1,XDDSLMessages.VARIABLE_NOT_TERMINATED,toString());
		}
		String name = null;
		String defaultValue = null;
		if (defaultValueStart == -1) {
			name = value.substring(startIndex+2,pos-1);
		} else {
			name = value.substring(startIndex+2,defaultValueStart-1);
			defaultValue = value.substring(defaultValueStart,pos-1);
		}
		return new Variable(name,defaultValue,startIndex,pos-1);
	}
	
	private List<Variable> getVariablesInValue() {
		List<Variable> variables = null;
		int idx = value.indexOf("${");
		while (idx!=-1) {
			// TODO Check for escaping
			Variable v = locateVariable(idx);
			if (variables==null) {
				 variables = new ArrayList<Variable>();
			}
			variables.add(v);
			idx = value.indexOf("${",v.end+1);
		}
		return (variables==null?Collections.<Variable>emptyList():variables);
	}

}
