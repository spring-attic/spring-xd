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
import static org.springframework.xd.dirt.stream.dsl.XDDSLMessages.Kind.ERROR;

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

	UNEXPECTED_DATA_AFTER_STREAMDEF(ERROR,100,"Found unexpected data after stream definition: ''{0}''"),//
	NO_WHITESPACE_BEFORE_ARG_NAME(ERROR,101,"No whitespace allowed between '--' and option name"),//
	NO_WHITESPACE_BEFORE_ARG_EQUALS(ERROR,102,"No whitespace allowed after argument name and before '='"),//
	NO_WHITESPACE_BEFORE_ARG_VALUE(ERROR,103,"No whitespace allowed after '=' and before option value"),//
	MORE_INPUT(ERROR,104, "After parsing a valid stream, there is still more data: ''{0}''"),
	EXPECTED_ARGUMENT_VALUE(ERROR,105,"Expected an argument value but was ''{0}''"),
	NON_TERMINATING_DOUBLE_QUOTED_STRING(ERROR,106,"Cannot find terminating \" for string"),//
	NON_TERMINATING_QUOTED_STRING(ERROR,107,"Cannot find terminating '' for string"), //
	MISSING_CHARACTER(ERROR,108,"missing expected character ''{0}''"),
	NOT_EXPECTED_TOKEN(ERROR,111,"Unexpected token.  Expected ''{0}'' but was ''{1}''"),
	OOD(ERROR,112,"Unexpectedly ran out of input"), //
	UNEXPECTED_ESCAPE_CHAR(ERROR,114,"unexpected escape character."), //
	UNEXPECTED_DATA(ERROR,115,"unexpected data in stream definition ''{0}''"), //
	UNRECOGNIZED_STREAM_REFERENCE(ERROR,116,"unrecognized stream reference ''{0}''"), //
	UNRECOGNIZED_MODULE_REFERENCE(ERROR,117,"unrecognized module reference ''{0}''"), //
	EXPECTED_MODULENAME(ERROR,118,"expected module name but found ''{0}''"), //
	EXPECTED_WHITESPACE_AFTER_MODULE_BEFORE_ARGUMENT(ERROR,119,"expected whitespace after module name and before argument"), //
	EXPECTED_CHANNEL_QUALIFIER(ERROR,120,"expected channel reference '':<channel>'' but found ''{0}''"), //
	EXPECTED_CHANNEL_NAME(ERROR,121,"expected channel name but found ''{0}''"), //
	ILLEGAL_STREAM_NAME(ERROR,122,"illegal name for a stream ''{0}''"), //
	NO_SOURCE_IN_SUBSTREAM(ERROR,123,"substreams cannot specify a source channel, substream is ''{0}''"), //
	NO_SINK_IN_SUBSTREAM(ERROR,124,"substreams cannot specify a sink channel, substream is ''{0}''"), //
	MISSING_VALUE_FOR_VARIABLE(ERROR,125,"no value specified for variable ''{0}'' when using substream"), //
	VARIABLE_NOT_TERMINATED(ERROR,126,"unable to find variable terminator ''}'' in argument ''{0}''"), //
//	ALREADY_HAS_SOURCE(ERROR,125,"cannot use substream with a source channel here"), //
//	ALREADY_HAS_SINK(ERROR,126,"cannot use substream with a sink channel here"), //
//	CANNOT_USE_SUBSTREAM_WITH_SOURCE(ERROR,127,"cannot use substream with source channel"), //
//	CANNOT_USE_SUBSTREAM_WITH_SINK(ERROR,128,"cannot use substream with source channel"), //
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
