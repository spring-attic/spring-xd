/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.job.dsl;

import java.util.List;

/**
 * Common AST base class for nodes representing job definitions or job references.
 *
 * @author Andy Clement
 */
public abstract class JobDescriptor extends JobNode {

	/**
	 * Both inline job definitions and job references can be suffixed with transitions. For example "<tt>| completed=foo</tt>" which
	 * indicates if the job finishes in the <tt>completed</tt> state then execution should continue with the <tt>foo</tt> job.
	 */
	List<Transition> transitions;

	ArgumentNode[] args;

	public JobDescriptor(int startpos, int endpos, ArgumentNode[] args) {
		super(startpos, endpos);
		this.args = args;
	}

	void setTransitions(List<Transition> transitions) {
		this.transitions = transitions;
	}

	@Override
	public String toString() {
		return "JobDescriptor: " + stringify(true);
	}

	@Override
	public final boolean isJobDescriptor() {
		return true;
	}

	public boolean isReference() {
		return false;
	}

	public boolean isDefinition() {
		return false;
	}

	public List<Transition> getTransitions() {
		return transitions;
	}

	public boolean hasTransitions() {
		return transitions != null;
	}

	public boolean hasArguments() {
		return args != null;
	}

	public ArgumentNode[] getArguments() {
		return args;
	}

	public abstract String getName();

}
