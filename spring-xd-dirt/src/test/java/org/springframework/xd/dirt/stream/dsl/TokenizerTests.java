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

import java.util.List;

import org.junit.Test;

/**
 * @author Andy Clement
 */
public class TokenizerTests {

	@Test
	public void oneModule() {
		Tokenizer t = new Tokenizer("foo");
		checkTokens(t,token_identifier("foo",0,3));
	}

	@Test
	public void moduleAliasing() {
		Tokenizer t = new Tokenizer("mystream = foo");
		checkTokens(t,token_identifier("mystream",0,8),token(TokenKind.EQUALS,9,10),token_identifier("foo",11,14));
	}

	@Test
	public void dirtyTap() {
		Tokenizer t = new Tokenizer("tap one.foo");
		checkTokens(t,token_identifier("tap",0,3),token_identifier("one",4,7),token(TokenKind.DOT,7,8),token_identifier("foo",8,11));
	}

	@Test
	public void moduleAliasingWithOptions() {
		Tokenizer t = new Tokenizer("myhttp = http --port=9090");
		checkTokens(t,token_identifier("myhttp",0,6),token(TokenKind.EQUALS,7,8),token_identifier("http",9,13),
				token(TokenKind.DOUBLE_MINUS,14,16),token_identifier("port",16,20),token(TokenKind.EQUALS,20,21),token_identifier("9090",21,25));
	}

	@Test
	public void twoModules() {
		Tokenizer t = new Tokenizer("foo | bar");
		checkTokens(t,token_identifier("foo",0,3),token(TokenKind.PIPE,4,5),token_identifier("bar",6,9));
	}

	@Test
	public void threeModules() {
		Tokenizer t = new Tokenizer("foo | bar | goo");
		checkTokens(t,token_identifier("foo",0,3),token(TokenKind.PIPE,4,5),token_identifier("bar",6,9),token(TokenKind.PIPE,10,11),token_identifier("goo",12,15));
	}

	@Test
	public void oneModuleWithParam() {
		Tokenizer t = new Tokenizer("foo --name=value");
		checkTokens(t,token_identifier("foo",0,3),token(TokenKind.DOUBLE_MINUS,4,6),token_identifier("name",6,10),token(TokenKind.EQUALS,10,11),token_identifier("value",11,16));
	}

	@Test
	public void oneModuleWithTwoParam() {
		Tokenizer t = new Tokenizer("foo --name=value --xx=yy");
		checkTokens(t,token_identifier("foo",0,3),
				token(TokenKind.DOUBLE_MINUS,4,6),token_identifier("name",6,10),token(TokenKind.EQUALS,10,11),token_identifier("value",11,16),
				token(TokenKind.DOUBLE_MINUS,17,19),token_identifier("xx",19,21),token(TokenKind.EQUALS,21,22),token_identifier("yy",22,24));
	}

	@Test
	public void messyArgumentValues() {
		Tokenizer t = new Tokenizer("foo --name=4:5abcdef --xx=(aaa)bbc%32");
		checkTokens(t,token_identifier("foo",0,3),
				token(TokenKind.DOUBLE_MINUS,4,6),token_identifier("name",6,10),token(TokenKind.EQUALS,10,11),token_identifier("4:5abcdef",11,20),
				token(TokenKind.DOUBLE_MINUS,21,23),token_identifier("xx",23,25),token(TokenKind.EQUALS,25,26),token_identifier("(aaa)bbc%32",26,37));
	}

	@Test
	public void newlines() {
		Tokenizer t = new Tokenizer("foo\nbar");
		checkTokens(t,token_identifier("foo",0,3),token(TokenKind.NEWLINE,3,4),token_identifier("bar",4,7));
	}

	@Test
	public void semicolons() {
		Tokenizer t = new Tokenizer("foo;bar");
		checkTokens(t,token_identifier("foo",0,3),token(TokenKind.SEMICOLON,3,4),token_identifier("bar",4,7));
	}

	@Test
	public void oneModuleWithSpacedValues() {
		Tokenizer t = new Tokenizer("foo --name='i am a foo'");
		checkTokens(t,token_identifier("foo",0,3),token(TokenKind.DOUBLE_MINUS,4,6),token_identifier("name",6,10),
				token(TokenKind.EQUALS,10,11),token(TokenKind.LITERAL_STRING,"'i am a foo'",11,23));

		t = new Tokenizer("foo --name='i have a ''string'' inside'");
		checkTokens(t,token_identifier("foo",0,3),token(TokenKind.DOUBLE_MINUS,4,6),token_identifier("name",6,10),
				token(TokenKind.EQUALS,10,11),token(TokenKind.LITERAL_STRING,"'i have a ''string'' inside'",11,39));

		t = new Tokenizer("foo --name=\"i have a 'string' inside\"");
		checkTokens(t,token_identifier("foo",0,3),token(TokenKind.DOUBLE_MINUS,4,6),token_identifier("name",6,10),
				token(TokenKind.EQUALS,10,11),token(TokenKind.LITERAL_STRING,"\"i have a 'string' inside\"",11,37));

		t = new Tokenizer("foo --name='i have a \"string\" inside'");
		checkTokens(t,token_identifier("foo",0,3),token(TokenKind.DOUBLE_MINUS,4,6),token_identifier("name",6,10),
				token(TokenKind.EQUALS,10,11),token(TokenKind.LITERAL_STRING,"'i have a \"string\" inside'",11,37));
	}

	// ---

	private void checkTokens(Tokenizer tokenizer, Token... expectedTokens) {
		List<Token> tokens = tokenizer.getTokens();
		if (tokens.size() != expectedTokens.length) {
			fail("Expected stream\n"+stringify(expectedTokens)+"\n but found\n"+stringify(tokens));
		}
		for (int t=0;t<expectedTokens.length;t++) {
			if (!expectedTokens[t].equals(tokens.get(t))) {
				fail("Token #"+t+" is not as expected. Expected "+expectedTokens[t]+" but was "+tokens.get(t));
			}
		}
	}

	private Token token(TokenKind tokenkind, String data, int start, int end) {
		return new Token(tokenkind, data.toCharArray(), start, end);
	}

	private Token token(TokenKind tokenkind, int start, int end) {
		return new Token(tokenkind, start, end);
	}

	private Token token_identifier(String identifier, int start, int end) {
		return new Token(TokenKind.IDENTIFIER, identifier.toCharArray(), start, end);
	}

	private String stringify(Token[] tokens) {
		StringBuilder s = new StringBuilder();
		if (tokens == null) {
			s.append("null");
		} else {
			s.append("#").append(tokens.length).append(" ");
			for (Token token: tokens) {
				s.append(token).append(" ");
			}
		}
		return s.toString().trim();
	}

	private String stringify(List<Token> tokens) {
		if (tokens == null) return null;
		return stringify(tokens.toArray(new Token[tokens.size()]));
	}

}