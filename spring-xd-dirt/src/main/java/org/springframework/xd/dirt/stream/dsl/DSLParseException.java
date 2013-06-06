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

import org.springframework.expression.ParseException;

/**
 * Root exception for DSL parsing related exceptions. Rather than holding a hard coded string indicating the problem, it
 * records a message key and the inserts for the message. See {@link XDDSLMessages} for the list of all possible messages
 * that can occur.
 *
 * @author Andy Clement
 */
@SuppressWarnings("serial")
public class DSLParseException extends ParseException {

	private XDDSLMessages message;
	private Object[] inserts;

	public DSLParseException(String expressionString, int position, XDDSLMessages message, Object... inserts) {
		super(expressionString, position, message.formatMessage(position,inserts));
		this.position = position;
		this.message = message;
		this.inserts = inserts;
	}

	public DSLParseException(int position, XDDSLMessages message, Object... inserts) {
		super(position, message.formatMessage(position,inserts));
		this.position = position;
		this.message = message;
		this.inserts = inserts;
	}

	public DSLParseException(int position, Throwable cause, XDDSLMessages message, Object... inserts) {
		super(position, message.formatMessage(position,inserts), cause);
		this.position = position;
		this.message = message;
		this.inserts = inserts;
	}

	/**
	 * @return a formatted message with inserts applied
	 */
	@Override
	public String getMessage() {
		if (message != null)
			return message.formatMessage(position, inserts);
		else
			return super.getMessage();
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

}