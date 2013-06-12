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

import java.text.MessageFormat;

/**
 * Contains all the messages that can be produced during Spring XD DSL parsing. Each message has a kind (info,
 * warn, error) and a code number. Tests can be written to expect particular code numbers rather than particular text,
 * enabling the message text to more easily be modified and the tests to run successfully in different locales.
 * <p>
 * When a message is formatted, it will have this kind of form
 *
 * <pre class="code">
 * XD105E: (pos 34): Expected an argument value but was ' '
 * </pre>
 *
 * </code> The prefix captures the code and the error kind, whilst the position is included if it is known.
 *
 * @author Andy Clement
 */
public enum XDDSLMessages {

	// TODO [Andy] need testcases for all these kinds of message
	UNEXPECTED_DATA_AFTER_MODULE(Kind.ERROR,100,"Unexpected data after module definition: ''{0}''"),//
	NO_WHITESPACE_BEFORE_ARG_NAME(Kind.ERROR,101,"No whitespace allowed between '--' and option name"),//
	NO_WHITESPACE_BEFORE_ARG_EQUALS(Kind.ERROR,102,"No whitespace allowed after argument name and before '='"),//
	NO_WHITESPACE_BEFORE_ARG_VALUE(Kind.ERROR,103,"No whitespace allowed after '=' and before option value"),//
	MORE_INPUT(Kind.ERROR,104, "After parsing a valid stream, there is still more data: ''{0}''"),
	EXPECTED_ARGUMENT_VALUE(Kind.ERROR,105,"Expected an argument value but was ''{0}''"),
	NON_TERMINATING_DOUBLE_QUOTED_STRING(Kind.ERROR,106,"Cannot find terminating \" for string"),//
	NON_TERMINATING_QUOTED_STRING(Kind.ERROR,107,"Cannot find terminating '' for string"), //
	MISSING_CHARACTER(Kind.ERROR,108,"missing expected character ''{0}''"),
	NOT_EXPECTED_TOKEN(Kind.ERROR,111,"Unexpected token.  Expected ''{0}'' but was ''{1}''"),
	OOD(Kind.ERROR,112,"Unexpectedly ran out of input"), //
	UNEXPECTED_ESCAPE_CHAR(Kind.ERROR,114,"unexpected escape character."), //
	UNEXPECTED_DATA(Kind.ERROR,115,"unexpected data in stream configuration ''{0}''"), //
	UNRECOGNIZED_STREAM_REFERENCE(Kind.ERROR,116,"unrecognized stream reference ''{0}''"), //
	UNRECOGNIZED_MODULE_REFERENCE(Kind.ERROR,117,"unrecognized module reference ''{0}''"), //
	;

	private Kind kind;
	private int code;
	private String message;

	private XDDSLMessages(Kind kind, int code, String message) {
		this.kind = kind;
		this.code = code;
		this.message = message;
	}

	/**
	 * Produce a complete message including the prefix, the position (if known) and with the inserts applied to the
	 * message.
	 *
	 * @param pos the position, if less than zero it is ignored and not included in the message
	 * @param inserts the inserts to put into the formatted message
	 * @return a formatted message
	 */
	public String formatMessage(int pos, Object... inserts) {
		StringBuilder formattedMessage = new StringBuilder();
		formattedMessage.append("XD").append(code);
//		switch (kind) {
//		case WARNING:
//			formattedMessage.append("W");
//			break;
//		case INFO:
//			formattedMessage.append("I");
//			break;
//		case ERROR:
		formattedMessage.append("E");
//			break;
//		}
		formattedMessage.append(":");
		if (pos != -1) {
			formattedMessage.append("(pos ").append(pos).append("): ");
		}
		formattedMessage.append(MessageFormat.format(message, inserts));
		return formattedMessage.toString();
	}

	public Kind getKind() {
		return kind;
	}

	public static enum Kind {
		INFO, WARNING, ERROR
	}

}
