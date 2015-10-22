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
 * Represents an inlined job definition in a job specification. For example in the specification
 * "<tt>aa || bb || cc dd --a=b --c=d || ff</tt>" a Job definition would be created to represent
 * <tt>cc</tt>. It would be a definition where the jobModuleId is <tt>cc</tt>, the jobNameId is <tt>dd</tt>
 * and the arguments are <tt>a=b</tt> and <tt>c=d</tt>.
 *
 * @author Andy Clement
 */
public class JobDefinition extends JobDescriptor {

	private Token jobModuleId;

	private Token jobNameId;

	public JobDefinition(Token jobModuleId, Token jobNameId, ArgumentNode[] args) {
		super(jobModuleId.startpos, jobNameId.endpos, args);
		this.jobModuleId = jobModuleId;
		this.jobNameId = jobNameId;
	}

	@Override
	public String stringify(boolean includePositionInfo) {
		StringBuilder s = new StringBuilder();
		s.append(jobModuleId.stringValue()).append(" ").append(jobNameId.stringValue());
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
	public final boolean isDefinition() {
		return true;
	}

	public String getJobModuleName() {
		return jobModuleId.stringValue();
	}

	public String getJobName() {
		return jobNameId.stringValue();
	}

	@Override
	public String getName() {
		return getJobName();
	}

}
