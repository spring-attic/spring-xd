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

/**
 * Enumeration of all the token types that may be found in a Job DSL specification.
 *
 * @author Andy Clement
 */
public enum TokenKind {
	IDENTIFIER, //
	AMPERSAND("&"), //
	DOUBLE_PIPE("||"), //
	DOUBLE_MINUS("--"), //
	OPEN_PAREN("("), //
	CLOSE_PAREN(")"), //
	EQUALS("="), //
	PIPE("|"), //
	NEWLINE("\n"), //
	SPLIT_OPEN("<"), //
	SPLIT_CLOSE(">"), //
	LITERAL_STRING,; //

	char[] tokenChars;

	private boolean hasPayload; // is there more to this token than simply the kind

	private TokenKind(String tokenString) {
		tokenChars = tokenString.toCharArray();
		hasPayload = tokenChars.length == 0;
	}

	private TokenKind() {
		this("");
	}

	@Override
	public String toString() {
		return this.name() + (tokenChars.length != 0 ? "(" + new String(tokenChars) + ")" : "");
	}

	public boolean hasPayload() {
		return hasPayload;
	}

	public int getLength() {
		return tokenChars.length;
	}

	/**
	 * @return the chars representing simple fixed token (eg. : > --)
	 */
	public char[] getTokenChars() {
		return tokenChars;
	}

	public String getTokenString() {
		return new String(tokenChars);
	}
}
