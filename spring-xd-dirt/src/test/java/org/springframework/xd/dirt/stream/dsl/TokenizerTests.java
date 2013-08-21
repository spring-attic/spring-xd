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

import static org.junit.Assert.fail;
import static org.springframework.xd.dirt.stream.dsl.TokenKind.COLON;
import static org.springframework.xd.dirt.stream.dsl.TokenKind.DOT;
import static org.springframework.xd.dirt.stream.dsl.TokenKind.DOUBLE_MINUS;
import static org.springframework.xd.dirt.stream.dsl.TokenKind.EQUALS;
import static org.springframework.xd.dirt.stream.dsl.TokenKind.GT;
import static org.springframework.xd.dirt.stream.dsl.TokenKind.IDENTIFIER;
import static org.springframework.xd.dirt.stream.dsl.TokenKind.LITERAL_STRING;
import static org.springframework.xd.dirt.stream.dsl.TokenKind.NEWLINE;
import static org.springframework.xd.dirt.stream.dsl.TokenKind.PIPE;
import static org.springframework.xd.dirt.stream.dsl.TokenKind.SEMICOLON;

import java.util.List;

import org.junit.Test;

/**
 * Check the tokenizer breaks strings up into correct token streams.
 * 
 * @author Andy Clement
 */
public class TokenizerTests {

	@Test
	public void oneModule() {
		Tokenizer t = new Tokenizer("foo");
		checkTokens(t, token_id("foo", 0, 3));
	}

	@Test
	public void moduleAliasing() {
		Tokenizer t = new Tokenizer("mystream = foo");
		checkTokens(t, token_id("mystream", 0, 8), token(EQUALS, 9, 10), token_id("foo", 11, 14));
	}

	@Test
	public void dirtyTap() {
		Tokenizer t = new Tokenizer("tap one.foo");
		checkTokens(t, token_id("tap", 0, 3), token_id("one", 4, 7), token(DOT, 7, 8), token_id("foo", 8, 11));
	}

	@Test
	public void moduleAliasingWithOptions() {
		Tokenizer t = new Tokenizer("myhttp = http --port=9090");
		checkTokens(t, token_id("myhttp", 0, 6), token(EQUALS, 7, 8), token_id("http", 9, 13),
				token(DOUBLE_MINUS, 14, 16), token_id("port", 16, 20), token(EQUALS, 20, 21), token_id("9090", 21, 25));
	}

	@Test
	public void twoModules() {
		Tokenizer t = new Tokenizer("foo | bar");
		checkTokens(t, token_id("foo", 0, 3), token(PIPE, 4, 5), token_id("bar", 6, 9));
	}

	@Test
	public void threeModules() {
		Tokenizer t = new Tokenizer("foo | bar | goo");
		checkTokens(t, token_id("foo", 0, 3), token(PIPE, 4, 5), token_id("bar", 6, 9), token(PIPE, 10, 11),
				token_id("goo", 12, 15));
	}

	@Test
	public void oneModuleWithParam() {
		Tokenizer t = new Tokenizer("foo --name=value");
		checkTokens(t, token_id("foo", 0, 3), token(DOUBLE_MINUS, 4, 6), token_id("name", 6, 10),
				token(EQUALS, 10, 11), token_id("value", 11, 16));
	}

	@Test
	public void quotesInParam1() {
		Tokenizer t = new Tokenizer("foo --bar='payload.matches(''hello'')'");
		checkTokens(t, token_id("foo", 0, 3), token(DOUBLE_MINUS, 4, 6), token_id("bar", 6, 9), token(EQUALS, 9, 10),
				token(LITERAL_STRING, "'payload.matches(''hello'')'", 10, 38));
	}

	@Test
	public void quotesInParam2() {
		Tokenizer t = new Tokenizer("foo --bar=payload.matches('hello')");
		checkTokens(t, token_id("foo", 0, 3), token(DOUBLE_MINUS, 4, 6), token_id("bar", 6, 9), token(EQUALS, 9, 10),
				token_id("payload.matches('hello')", 10, 34));
	}

	@Test
	public void quotesInParam3() {
		Tokenizer t = new Tokenizer("foo --bar=payload.matches('hello world')");
		checkTokens(t, token_id("foo", 0, 3), token(DOUBLE_MINUS, 4, 6), token_id("bar", 6, 9), token(EQUALS, 9, 10),
				token_id("payload.matches('hello world')", 10, 40));
	}

	@Test
	public void quotesInParam4() {
		Tokenizer t = new Tokenizer("foo --bar=payload.matches('helloworld')");
		checkTokens(t, token_id("foo", 0, 3), token(DOUBLE_MINUS, 4, 6), token_id("bar", 6, 9), token(EQUALS, 9, 10),
				token_id("payload.matches('helloworld')", 10, 39));
	}

	@Test
	public void quotesInParam5() {
		Tokenizer t = new Tokenizer("foo --bar=payload.matches('hello\tworld')");
		checkTokens(t, token_id("foo", 0, 3), token(DOUBLE_MINUS, 4, 6), token_id("bar", 6, 9), token(EQUALS, 9, 10),
				token_id("payload.matches('hello\tworld')", 10, 40));
	}

	@Test
	public void quotesInParam7() {
		Tokenizer t = new Tokenizer("foo --bar=payload.matches(\"'\")");
		checkTokens(t, token_id("foo", 0, 3), token(DOUBLE_MINUS, 4, 6), token_id("bar", 6, 9), token(EQUALS, 9, 10),
				token_id("payload.matches(\"'\")", 10, 30));
	}

	@Test
	public void quotesInParam6() {
		Tokenizer t = new Tokenizer("foo --bar=payload.matches(\"hello\t  world\")");
		checkTokens(t, token_id("foo", 0, 3), token(DOUBLE_MINUS, 4, 6), token_id("bar", 6, 9), token(EQUALS, 9, 10),
				token_id("payload.matches(\"hello\t  world\")", 10, 42));
	}

	@Test
	public void oneModuleWithTwoParam() {
		Tokenizer t = new Tokenizer("foo --name=value --xx=yy");
		checkTokens(t, token_id("foo", 0, 3),
				token(DOUBLE_MINUS, 4, 6), token_id("name", 6, 10), token(EQUALS, 10, 11), token_id("value", 11, 16),
				token(DOUBLE_MINUS, 17, 19), token_id("xx", 19, 21), token(EQUALS, 21, 22), token_id("yy", 22, 24));
	}

	@Test
	public void messyArgumentValues() {
		Tokenizer t = new Tokenizer("foo --name=4:5abcdef --xx=(aaa)bbc%32");
		checkTokens(t, token_id("foo", 0, 3),
				token(DOUBLE_MINUS, 4, 6), token_id("name", 6, 10), token(EQUALS, 10, 11),
				token_id("4:5abcdef", 11, 20),
				token(DOUBLE_MINUS, 21, 23), token_id("xx", 23, 25), token(EQUALS, 25, 26),
				token_id("(aaa)bbc%32", 26, 37));
	}

	@Test
	public void newlines() {
		Tokenizer t = new Tokenizer("foo\nbar");
		checkTokens(t, token_id("foo", 0, 3), token(NEWLINE, 3, 4), token_id("bar", 4, 7));
	}

	@Test
	public void semicolons() {
		Tokenizer t = new Tokenizer("foo;bar");
		checkTokens(t, token_id("foo", 0, 3), token(SEMICOLON, 3, 4), token_id("bar", 4, 7));
	}

	@Test
	public void channels() {
		Tokenizer t = new Tokenizer(":a.b > c | d > :e");
		checkTokens(t, token(COLON, 0, 1), token_id("a", 1, 2), token(DOT, 2, 3), token_id("b", 3, 4), token(GT, 5, 6),
				token_id("c", 7, 8), token(PIPE, 9, 10), token_id("d", 11, 12), token(GT, 13, 14),
				token(COLON, 15, 16), token_id("e", 16, 17));
	}

	@Test
	public void oneModuleWithSpacedValues() {
		Tokenizer t = new Tokenizer("foo --name='i am a foo'");
		checkTokens(t, token_id("foo", 0, 3), token(DOUBLE_MINUS, 4, 6), token_id("name", 6, 10),
				token(EQUALS, 10, 11), token(LITERAL_STRING, "'i am a foo'", 11, 23));

		t = new Tokenizer("foo --name='i have a ''string'' inside'");
		checkTokens(t, token_id("foo", 0, 3), token(DOUBLE_MINUS, 4, 6), token_id("name", 6, 10),
				token(EQUALS, 10, 11), token(LITERAL_STRING, "'i have a ''string'' inside'", 11, 39));

		t = new Tokenizer("foo --name=\"i have a 'string' inside\"");
		checkTokens(t, token_id("foo", 0, 3), token(DOUBLE_MINUS, 4, 6), token_id("name", 6, 10),
				token(EQUALS, 10, 11), token(LITERAL_STRING, "\"i have a 'string' inside\"", 11, 37));

		t = new Tokenizer("foo --name='i have a \"string\" inside'");
		checkTokens(t, token_id("foo", 0, 3), token(DOUBLE_MINUS, 4, 6), token_id("name", 6, 10),
				token(EQUALS, 10, 11), token(LITERAL_STRING, "'i have a \"string\" inside'", 11, 37));
	}

	// ---

	private void checkTokens(Tokenizer tokenizer, Token... expectedTokens) {
		List<Token> tokens = tokenizer.getTokens();
		if (tokens.size() != expectedTokens.length) {
			fail("Expected stream\n" + stringify(expectedTokens) + "\n but found\n" + stringify(tokens));
		}
		for (int t = 0; t < expectedTokens.length; t++) {
			if (!expectedTokens[t].equals(tokens.get(t))) {
				fail("Token #" + t + " is not as expected. Expected " + expectedTokens[t] + " but was " + tokens.get(t));
			}
		}
	}

	private Token token(TokenKind tokenkind, String data, int start, int end) {
		return new Token(tokenkind, data.toCharArray(), start, end);
	}

	private Token token(TokenKind tokenkind, int start, int end) {
		return new Token(tokenkind, start, end);
	}

	private Token token_id(String identifier, int start, int end) {
		return new Token(IDENTIFIER, identifier.toCharArray(), start, end);
	}

	private String stringify(Token[] tokens) {
		StringBuilder s = new StringBuilder();
		if (tokens == null) {
			s.append("null");
		}
		else {
			s.append("#").append(tokens.length).append(" ");
			for (Token token : tokens) {
				s.append(token).append(" ");
			}
		}
		return s.toString().trim();
	}

	private String stringify(List<Token> tokens) {
		if (tokens == null)
			return null;
		return stringify(tokens.toArray(new Token[tokens.size()]));
	}

}
