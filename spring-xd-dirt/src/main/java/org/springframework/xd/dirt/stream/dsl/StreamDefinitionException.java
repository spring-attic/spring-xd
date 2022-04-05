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

/**
 * Root exception for DSL parsing related exceptions. Rather than holding a hard coded string indicating the problem, it
 * records a message key and the inserts for the message. See {@link XDDSLMessages} for the list of all possible
 * messages that can occur.
 * 
 * @author Andy Clement
 */
@SuppressWarnings("serial")
public class StreamDefinitionException extends RuntimeException {

	protected String expressionString;

	protected int position; // -1 if not known - but should be known in all reasonable cases

	protected XDDSLMessages message;

	protected Object[] inserts;

	public StreamDefinitionException(String expressionString, int position, XDDSLMessages message, Object... inserts) {
		super(message.formatMessage(position, inserts));
		this.position = position;
		this.message = message;
		this.inserts = inserts;
		this.expressionString = expressionString;
	}

	/**
	 * @return a formatted message with inserts applied
	 */
	@Override
	public String getMessage() {
		StringBuilder s = new StringBuilder();
		if (message != null) {
			s.append(message.formatMessage(position, inserts));
		}
		else {
			s.append(super.getMessage());
		}
		if (expressionString != null && expressionString.length() > 0) {
			s.append("\n").append(expressionString).append("\n");
		}
		if (position >= 0) {
			for (int i = 0; i < position; i++) {
				s.append(' ');
			}
			s.append("^\n");
		}
		return s.toString();
	}

	/**
	 * @return the message code
	 */
	public XDDSLMessages getMessageCode() {
		return this.message;
	}

	/**
	 * @return the message inserts
	 */
	public Object[] getInserts() {
		return inserts;
	}

	/**
	 * @return the dsl expression text
	 */
	public final String getExpressionString() {
		return this.expressionString;
	}

	/**
	 * @return location of the error in the expression text
	 */
	public final int getPosition() {
		return position;
	}

}
