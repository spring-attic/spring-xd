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

import java.util.List;


public class CheckpointedStreamDefinitionException extends StreamDefinitionException {

	private int checkpointPointer = -1;

	private List<Token> tokens;


	public CheckpointedStreamDefinitionException(String expressionString, int position, int checkpointPointer,
			List<Token> tokens, XDDSLMessages message,
			Object... inserts) {
		super(expressionString, position, message, inserts);
		this.checkpointPointer = checkpointPointer;
		this.tokens = tokens;
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
		int offset = position;
		if (checkpointPointer > 0 && offset >= 0) {
			int checkpointPosition = getCheckpointPosition();
			offset -= checkpointPosition;
			for (int i = 0; i < checkpointPosition; i++) {
				s.append(' ');
			}
			s.append("*");
			offset--; // account for the '*'
		}
		if (offset >= 0) {
			for (int i = 0; i < offset; i++) {
				s.append(' ');
			}
			s.append("^\n");
		}
		return s.toString();
	}


	public int getCheckpointPosition() {
		return checkpointPointer == 0 ? 0 : tokens.get(checkpointPointer - 1).endpos;
	}

	/**
	 * Return the parsed expression until the last known, well formed position. Attempting to re-parse that expression
	 * is guaranteed to not fail.
	 */
	public String getExpressioStringUntilCheckpoint() {
		return expressionString.substring(0, getCheckpointPosition());
	}

	public int getCheckpointPointer() {
		return checkpointPointer;
	}


	public List<Token> getTokens() {
		return tokens;
	}

}
