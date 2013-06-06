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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.util.Assert;

// TODO [Andy] more general than SpringXD requires, trim it down once the spec is more well defined
/**
 * Lex some input data into a stream of tokens that can then be parsed.
 *
 * @author Andy Clement
 */
class Tokenizer {

	// The string to be tokenized
	String expressionString;
	char[] toProcess;

	// Length of input data
	int max;

	// Current lexing position in the input data
	int pos;

	// Output stream of tokens
	List<Token> tokens = new ArrayList<Token>();

	public Tokenizer(String inputdata) {
		this.expressionString = inputdata;
		this.toProcess = (inputdata+"\0").toCharArray();
		this.max = toProcess.length;
		this.pos = 0;
		process();
	}

	public void process() {
		boolean justProcessedEquals = false;
		while (pos<max) {
			char ch = toProcess[pos];

			if (justProcessedEquals) {
				if (!isWhitespace(ch) && !isQuote(ch)) {
					// following an '=' we commence a variant of regular tokenization, here we consume
					// everything up to the next pipe/whitespace
					lexArgValueIdentifier();
				}
				justProcessedEquals=false;
				continue;
			}

			if (isAlphabetic(ch) || isDigit(ch) || ch=='_') {
				lexIdentifier();
			} else {
				switch (ch) {
				case '-':
					if (!isTwoCharToken(TokenKind.DOUBLE_MINUS)) {
						throw new InternalParseException(new DSLParseException(
								expressionString, pos,
								XDDSLMessages.MISSING_CHARACTER, "-"));
					}
					pushPairToken(TokenKind.DOUBLE_MINUS);
					break;
				case '=':
					justProcessedEquals=true;
					pushCharToken(TokenKind.EQUALS);
					break;
				case '|':
					pushCharToken(TokenKind.PIPE);
					break;
				case ' ':
				case '\t':
				case '\r':
					// drift over white space
					pos++;
					break;
				case '\n':
					pushCharToken(TokenKind.NEWLINE);
					break;
				case ';':
					pushCharToken(TokenKind.SEMICOLON);
					break;
				case '\'':
					lexQuotedStringLiteral();
					break;
				case '"':
					lexDoubleQuotedStringLiteral();
					break;
				case '@':
					pushCharToken(TokenKind.REFERENCE);
					break;
				case 0:
					// hit sentinel at end of value
					pos++; // will take us to the end
					break;
				case '\\':
					throw new InternalParseException(new DSLParseException(expressionString,pos,XDDSLMessages.UNEXPECTED_ESCAPE_CHAR));
				default:
					throw new InternalParseException(new DSLParseException(expressionString,pos,XDDSLMessages.UNEXPECTED_DATA,ch));
//					throw new IllegalStateException("Cannot handle ("+Integer.valueOf(ch)+") '"+ch+"'");
				}
			}
		}
	}

	public List<Token> getTokens() {
		return tokens;
	}

	// STRING_LITERAL: '\''! (APOS|~'\'')* '\''!;
	private void lexQuotedStringLiteral() {
		int start = pos;
		boolean terminated = false;
		while (!terminated) {
			pos++;
			char ch = toProcess[pos];
			if (ch=='\'') {
				// may not be the end if the char after is also a '
				if (toProcess[pos+1]=='\'') {
					pos++; // skip over that too, and continue
				} else {
					terminated = true;
				}
			}
			if (ch==0) {
				throw new InternalParseException(new DSLParseException(expressionString,start,XDDSLMessages.NON_TERMINATING_QUOTED_STRING));
			}
		}
		pos++;
		tokens.add(new Token(TokenKind.LITERAL_STRING, subarray(start,pos), start, pos));
	}

	// DQ_STRING_LITERAL:	'"'! (~'"')* '"'!;
	private void lexDoubleQuotedStringLiteral() {
		int start = pos;
		boolean terminated = false;
		while (!terminated) {
			pos++;
			char ch = toProcess[pos];
			if (ch=='"') {
				// may not be the end if the char after is also a "
				if (toProcess[pos+1]=='"') {
					pos++; // skip over that too, and continue
				} else {
					terminated = true;
				}
			}
			if (ch==0) {
				throw new InternalParseException(new DSLParseException(expressionString,start,XDDSLMessages.NON_TERMINATING_DOUBLE_QUOTED_STRING));
			}
		}
		pos++;
		tokens.add(new Token(TokenKind.LITERAL_STRING, subarray(start,pos), start, pos));
	}

	// if this is changed, it must remain sorted
	private static final String[] alternativeOperatorNames = { "DIV","EQ","GE","GT","LE","LT","MOD","NE","NOT"};

	private void lexIdentifier() {
		int start = pos;
		do {
			pos++;
		} while (isIdentifier(toProcess[pos]));
		char[] subarray = subarray(start,pos);

		// Check if this is the alternative (textual) representation of an operator (see alternativeOperatorNames)
		if ((pos-start)==2 || (pos-start)==3) {
			String asString = new String(subarray).toUpperCase();
			int idx = Arrays.binarySearch(alternativeOperatorNames,asString);
			if (idx>=0) {
				pushOneCharOrTwoCharToken(TokenKind.valueOf(asString),start,subarray);
				return;
			}
		}
		tokens.add(new Token(TokenKind.IDENTIFIER,subarray,start,pos));
	}

	private boolean isArgValueIdentifierTerminator(char ch) {
		return ch=='|' || ch==';' || ch=='\0' || isWhitespace(ch);
	}

	private void lexArgValueIdentifier() {
		int start = pos;
		do {
			pos++;
		} while (!isArgValueIdentifierTerminator(toProcess[pos]));
		char[] subarray = subarray(start,pos);
		tokens.add(new Token(TokenKind.IDENTIFIER,subarray,start,pos));
	}

	private char[] subarray(int start, int end) {
		char[] result = new char[end - start];
		System.arraycopy(toProcess, start, result, 0, end - start);
		return result;
	}

	/**
	 * Check if this might be a two character token.
	 */
	private boolean isTwoCharToken(TokenKind kind) {
		Assert.isTrue(kind.tokenChars.length == 2);
		Assert.isTrue(toProcess[pos] == kind.tokenChars[0]);
		return toProcess[pos+1] == kind.tokenChars[1];
	}

	/**
	 * Push a token of just one character in length.
	 */
	private void pushCharToken(TokenKind kind) {
		tokens.add(new Token(kind,pos,pos+1));
		pos++;
	}

	/**
	 * Push a token of two characters in length.
	 */
	private void pushPairToken(TokenKind kind) {
		tokens.add(new Token(kind,pos,pos+2));
		pos+=2;
	}

	private void pushOneCharOrTwoCharToken(TokenKind kind, int pos, char[] data) {
		tokens.add(new Token(kind,data,pos,pos+kind.getLength()));
	}

	//	ID:	('a'..'z'|'A'..'Z'|'_'|'$') ('a'..'z'|'A'..'Z'|'_'|'$'|'0'..'9'|DOT_ESCAPED|-)*;
	private boolean isIdentifier(char ch) {
		return isAlphabetic(ch) || isDigit(ch) || ch=='_' || ch=='$' || ch=='-';
	}

	private boolean isQuote(char ch) {
		return ch=='\'' || ch=='"';
	}

	private boolean isWhitespace(char ch) {
		return ch==' ' || ch=='\t' || ch=='\r' || ch=='\n';
	}

	private boolean isDigit(char ch) {
		if (ch>255) {
			return false;
		}
		return (flags[ch] & IS_DIGIT)!=0;
	}

	private boolean isAlphabetic(char ch) {
		if (ch>255) {
			return false;
		}
		return (flags[ch] & IS_ALPHA)!=0;
	}

	private static final byte flags[] = new byte[256];
	private static final byte IS_DIGIT=0x01;
	private static final byte IS_HEXDIGIT=0x02;
	private static final byte IS_ALPHA=0x04;

	static {
		for (int ch='0';ch<='9';ch++) {
			flags[ch]|=IS_DIGIT | IS_HEXDIGIT;
		}
		for (int ch='A';ch<='F';ch++) {
			flags[ch]|= IS_HEXDIGIT;
		}
		for (int ch='a';ch<='f';ch++) {
			flags[ch]|= IS_HEXDIGIT;
		}
		for (int ch='A';ch<='Z';ch++) {
			flags[ch]|= IS_ALPHA;
		}
		for (int ch='a';ch<='z';ch++) {
			flags[ch]|= IS_ALPHA;
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(this.expressionString).append("\n");
		for (int i=0;i<this.pos;i++) {
			s.append(" ");
		}
		s.append("^\n");
		s.append(tokens).append("\n");
		return s.toString();
	}

}
