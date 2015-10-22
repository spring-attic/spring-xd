/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.dirt.job.dsl;

import static org.springframework.xd.dirt.job.dsl.JobDSLMessage.Kind.ERROR;

import java.text.MessageFormat;

/**
 * Contains all the messages that can be produced during Spring XD Job DSL parsing. Each message has a kind (info, warn,
 * error) and a code number. Tests can be written to expect particular code numbers rather than particular text,
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
public enum JobDSLMessage {

	UNEXPECTED_DATA_AFTER_JOBSPEC(ERROR, 200, "Found unexpected data after job specification: ''{0}''"), //
	NO_WHITESPACE_BEFORE_ARG_NAME(ERROR, 201, "No whitespace allowed between '--' and option name"), //
	NO_WHITESPACE_BEFORE_ARG_EQUALS(ERROR, 202, "No whitespace allowed after argument name and before '='"), //
	NO_WHITESPACE_BEFORE_ARG_VALUE(ERROR, 203, "No whitespace allowed after '=' and before option value"), //
	EXPECTED_ARGUMENT_VALUE(ERROR, 204, "Expected an argument value but was ''{0}''"), //
	MISSING_CHARACTER(ERROR, 205, "missing expected character ''{0}''"), //
	NOT_EXPECTED_TOKEN(ERROR, 206, "Unexpected token.  Expected ''{0}'' but was ''{1}''"), //
	OOD(ERROR, 207, "Unexpectedly ran out of input"), //
	UNEXPECTED_DATA(ERROR, 208, "unexpected data in job definition ''{0}''"), //
	EXPECTED_WHITESPACE_AFTER_NAME_BEFORE_ARGUMENT(ERROR,
			209, "expected whitespace after job name and before argument"), //
	NO_WHITESPACE_IN_DOTTED_NAME(ERROR, 210, "No whitespace is allowed between dot and components of a name"), //
	MISSING_JOB_NAME_IN_INLINEJOBDEF(ERROR,
			211, "The job module must be followed by the job name before any job arguments"), //
	EXPECTED_TRANSITION_NAME(ERROR, 212, "Expected the name of a job exit state but found ''{0}''"), //
	EXPECTED_EQUALS_AFTER_TRANSITION_NAME(ERROR, 213, "Expected an equals after a job exit state but found ''{0}''"), //
	NON_TERMINATING_QUOTED_STRING(ERROR, 214, "Cannot find terminating '' for string"), //
	NON_TERMINATING_DOUBLE_QUOTED_STRING(ERROR, 215, "Cannot find terminating \" for string"), //
	UNEXPECTED_ESCAPE_CHAR(ERROR, 216, "unexpected escape character."), //
	MISSING_EQUALS_AFTER_TRANSITION_NAME(ERROR, 217, "Expected an equals after the transition ''{0}''"), //
	ONLY_ONE_AMPERSAND_REQUIRED(ERROR, 218, "Only a single '&' is required between jobs in a split"), //
	EXPECTED_JOB_REF_OR_DEF(ERROR, 19, "Expected job reference or definition"), //
	JOB_REF_DOES_NOT_SUPPORT_OPTIONS(ERROR, 220, "The job reference ''{0}'' used here does not allow options"), //
	EXPECTED_FLOW_OR_SPLIT_CHARS(ERROR, 221, "Expected flow '||' or split '&' next, not ''{0}''");


	private Kind kind;

	private int code;

	private String message;

	private JobDSLMessage(Kind kind, int code, String message) {
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
		// switch (kind) {
		// case WARNING:
		// formattedMessage.append("W");
		// break;
		// case INFO:
		// formattedMessage.append("I");
		// break;
		// case ERROR:
		formattedMessage.append("E");
		// break;
		// }
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
