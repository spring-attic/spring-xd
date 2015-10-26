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

import org.springframework.xd.dirt.stream.dsl.AstNode;

/**
 * An AST node representing a transition found in the parsed Job specification. A transition
 * is expressed in the form "<tt>| STATE = TARGET_JOB</tt>" and if the job attached to the
 * transition finishes in the specified <tt>STATE</tt> then the next job to run should be
 * the <tt>TARGET_JOB</tt>. If the state has a space in it (or other funky characters) it can
 * be quoted.
 *
 * @author Andy Clement
 */
public class Transition extends AstNode {

	public final static String FAIL = "$FAIL";

	public final static String END = "$END";

	private Token stateNameToken;

	private String stateName;

	private JobReference targetJobReference;

	public Transition(Token stateNameToken, JobReference targetJobReference) {
		super(stateNameToken.startpos, targetJobReference.getEndPos());
		this.stateNameToken = stateNameToken;
		this.targetJobReference = targetJobReference;
		// If it is quoted, strip them off to determine real stateName
		if (stateNameToken.isKind(TokenKind.LITERAL_STRING)) {
			String quotesUsed = stateNameToken.data.substring(0, 1);
			this.stateName = stateNameToken.data.substring(1, stateNameToken.data.length() - 1).replace(
					quotesUsed + quotesUsed, quotesUsed);
		}
		else {
			this.stateName = this.stateNameToken.stringValue();
		}
	}

	@Override
	public String stringify(boolean includePositionInfo) {
		StringBuilder s = new StringBuilder();
		s.append("| ");
		s.append(stateNameToken.stringValue()).append(" = ").append(targetJobReference.getName());
		return s.toString();
	}

	public String getStateName() {
		return stateName;
	}

	public String getTargetJobName() {
		return targetJobReference.getName();
	}

	/**
	 * Basic names do not need wrapping in quotes but special characters, like
	 * asterisk do. This returns the state name in a form suitable for inclusion
	 * in DSL text (so with the quotes if that's how it was specified
	 * when the Transition object was built).
	 * @return the transition name suitable for inclusion in the DSL
	 */
	public String getStateNameInDSLForm() {
		return stateNameToken.data;
	}

	/**
	 * Some target names for a transition are 'well known' like $FAIL and $END - these
	 * do not indicate a following job step, they instead indicate a termination state.
	 * @return true if the target of this transition is a special state ($FAIL/$END)
	 */
	public boolean isSpecialTransition() {
		return isFailTransition() || isEndTransition();
	}

	public boolean isFailTransition() {
		return getStateNameInDSLForm().equals(FAIL);
	}

	public boolean isEndTransition() {
		return getStateNameInDSLForm().equals(END);
	}

}
