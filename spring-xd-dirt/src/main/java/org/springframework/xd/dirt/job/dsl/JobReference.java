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

/**
 * Represents a simple job reference. In the example spec "<tt>aa || bb</tt>" both <tt>aa</tt> and <tt>bb</tt> would
 * be represented in the AST as JobReference nodes.
 *
 * @author Andy Clement
 */
public class JobReference extends JobDescriptor {

	private Token jobReference;

	public JobReference(Token jobReference, ArgumentNode[] args) {
		super(jobReference.startpos, jobReference.endpos, args);
		this.jobReference = jobReference;
	}

	@Override
	public String stringify(boolean includePositionInfo) {
		StringBuilder s = new StringBuilder();
		if (includePositionInfo) {
			s.append(jobReference.stringValue()).append("[").append(getStartPos()).append(">").append(
					getEndPos()).append("]");
		}
		else {
			s.append(jobReference.stringValue());
		}
		if (args != null) {
			for (ArgumentNode arg : args) {
				s.append(" ");
				s.append(arg.stringify(includePositionInfo));
			}
		}
		if (hasTransitions()) {
			for (Transition t : transitions) {
				s.append(" ");
				s.append(t.stringify(includePositionInfo));
			}
		}
		return s.toString();
	}

	@Override
	public String toString() {
		return "JobReference: " + stringify(true);
	}

	@Override
	public final boolean isReference() {
		return true;
	}

	/**
	 * @return the job named by this JobReference.
	 */
	public String getName() {
		return this.jobReference.stringValue();
	}
}
