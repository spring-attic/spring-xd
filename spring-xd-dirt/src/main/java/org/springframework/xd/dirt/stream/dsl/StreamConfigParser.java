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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.xd.dirt.stream.dsl.ast.ArgumentNode;
import org.springframework.xd.dirt.stream.dsl.ast.ModuleNode;
import org.springframework.xd.dirt.stream.dsl.ast.StreamNode;
import org.springframework.xd.dirt.stream.dsl.ast.StreamsNode;

// TODO [Andy] more flexible than we need right now, fix that up as DSL settles down
/**
 * @author Andy Clement
 */
public class StreamConfigParser {

	private String expressionString;
	private List<Token> tokenStream;
	private int tokenStreamLength;
	private int tokenStreamPointer; // Current location in the token stream when processing tokens
		
	private static Map<String,StreamsNode> knownExpressions = new HashMap<String,StreamsNode>();
	
	public StreamsNode parse(String stream) {
		return parse(null, stream);
	}

	public StreamsNode parse(String name, String stream) {
		try {
			this.expressionString = stream;
			Tokenizer tokenizer = new Tokenizer(expressionString);
			tokenStream = tokenizer.getTokens();
			tokenStreamLength = tokenStream.size();
			tokenStreamPointer = 0;
			StreamsNode ast = eatStreams();
			if (moreTokens()) {
				throw new DSLParseException(peekToken().startpos,XDDSLMessages.MORE_INPUT,toString(nextToken()));
			}
			if (name != null) {
				knownExpressions.put(name,ast);
			}
			return ast;
		} catch (InternalParseException ipe) {
			throw ipe.getCause();
		}
	}

	// streams: name = stream ([;\n] stream)*
	private StreamsNode eatStreams() {
		List<StreamNode> streamNodes = new ArrayList<StreamNode>();
		streamNodes.add(eatStream());
		while (peekToken(TokenKind.NEWLINE,TokenKind.SEMICOLON)) {
			nextToken();
			streamNodes.add(eatStream());
		}
		return new StreamsNode(this.expressionString, streamNodes);
	}

	// stream: name = module (| module)*
	private StreamNode eatStream() {
		List<ModuleNode> moduleNodes= new ArrayList<ModuleNode>();
		String streamName = null;
		// Is the stream named?
		if (lookAhead(1,TokenKind.EQUALS)) {
			if (peekToken(TokenKind.IDENTIFIER)) {
				streamName = eatToken(TokenKind.IDENTIFIER).data;
				nextToken(); // skip '='
			} else {
				// TODO [Andy] error: not a name we can use to name the stream
			}
		}
		moduleNodes.add(eatModule());
		while (moreTokens()) {
			Token t = peekToken();
			if (t.kind == TokenKind.PIPE) {
				nextToken();
				moduleNodes.add(eatModule());
			} else if (t.kind == TokenKind.NEWLINE || t.kind == TokenKind.SEMICOLON) {
				// end of this stream
				break;
			} else {
				raiseInternalException(t.startpos,XDDSLMessages.UNEXPECTED_DATA_AFTER_MODULE,toString(peekToken()));
			}
		}
		StreamNode streamNode= new StreamNode(streamName, moduleNodes);
		return streamNode;
	}

	// TODO [Andy] temporary to support horrid tap @ syntax
	boolean isTap = false;

	// module: identifier (moduleArguments)*
	private ModuleNode eatModule() {
		Token moduleName = eatToken(TokenKind.IDENTIFIER);
		isTap = (moduleName.data.equals("tap"));
		ArgumentNode[] args = maybeEatModuleArgs();
		return new ModuleNode(moduleName.data, moduleName.startpos, moduleName.endpos, args);
	}

	// moduleArguments : DOUBLE_MINUS identifier(name) EQUALS identifier(value)
	private ArgumentNode[] maybeEatModuleArgs() {
		List<ArgumentNode> args = null;
		if (isTap && !peekToken(TokenKind.REFERENCE)) {
			Token streamtoken = eatToken(TokenKind.IDENTIFIER);
			String streamname = streamtoken.data;
			// The syntax 'tap streamname.module'
			// optionQualifier is probably the streamname
			eatToken(TokenKind.DOT);
			Token moduletoken = eatToken(TokenKind.IDENTIFIER);
			String modulename = moduletoken.data;
			
			// Let's map the modulename
			StreamsNode existingAst = knownExpressions.get(streamname);
			if (existingAst == null) {
				raiseInternalException(streamtoken.startpos, XDDSLMessages.UNRECOGNIZED_STREAM_REFERENCE,streamname);
			}
			List<ModuleNode> modules = existingAst.getModuleNodes();
			boolean mappedToPipe = false;
			for (int m=0;m<modules.size();m++) {
				if (modules.get(m).getName().equals(modulename)) {
					modulename = Integer.toString(m);
					mappedToPipe = true;
				}
			}
			if (mappedToPipe) {
				args = new ArrayList<ArgumentNode>();
				args.add(new ArgumentNode("channel", streamname+"."+modulename, streamtoken.startpos, moduletoken.endpos));
			} else {
				raiseInternalException(moduletoken.startpos,  XDDSLMessages.UNRECOGNIZED_MODULE_REFERENCE, modulename);
			}
			return args==null?null:args.toArray(new ArgumentNode[args.size()]);
		}
		
		while (peekToken(TokenKind.DOUBLE_MINUS,TokenKind.REFERENCE)) {
			Token optionQualifier = nextToken(); // skip the '--' (or '@' at the moment...)
			
			// This is dirty, temporary, until we nail the tap syntax
			if (isTap) {
				if (optionQualifier.getKind()==TokenKind.REFERENCE) {
					Token t = peekToken();
					String argValue = eatArgValue(t);
					if (peekToken(TokenKind.DOT)) {
						// tap @foo.NNN
						nextToken();
						String channelNumber = eatToken(TokenKind.IDENTIFIER).data;
						argValue = argValue+"."+channelNumber;
					} else {
						// no channel number specified
						argValue = argValue+".0";
					}
					
					if (args == null) {
						args = new ArrayList<ArgumentNode>();
					}
					args.add(new ArgumentNode("channel", argValue, optionQualifier.startpos, t.endpos));
					continue;
				}
			}
			if (peekToken(TokenKind.IDENTIFIER) && !isNextTokenAdjacent()) {
				raiseInternalException(peekToken().startpos, XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_NAME);
			}
			Token argName = eatToken(TokenKind.IDENTIFIER);
			if (peekToken(TokenKind.EQUALS) && !isNextTokenAdjacent()) {
				raiseInternalException(peekToken().startpos, XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_EQUALS);
			}
			eatToken(TokenKind.EQUALS);
			if (peekToken(TokenKind.IDENTIFIER) && !isNextTokenAdjacent()) {
				raiseInternalException(peekToken().startpos, XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_VALUE);
			}
			// Process argument value:
			Token t = peekToken();
			String argValue = eatArgValue(t);
			
			if (args == null) {
				args = new ArrayList<ArgumentNode>();
			}
			args.add(new ArgumentNode(argName.data, argValue, argName.startpos-2, t.endpos));
		}
		return args==null?null:args.toArray(new ArgumentNode[args.size()]);
	}

	// argValue: identifier | literal_string
	private String eatArgValue(Token t) {
		String argValue = null;
		if (t.getKind()==TokenKind.IDENTIFIER) {
			argValue = t.data;
		} else if (t.getKind() == TokenKind.LITERAL_STRING) {
			argValue = t.data.substring(1,t.data.length()-1).replaceAll("''", "'").replaceAll("\"\"", "\"");
		} else {
			raiseInternalException(t.startpos,XDDSLMessages.EXPECTED_ARGUMENT_VALUE,t.data);
		}
		nextToken();
		return argValue;
	}

	private Token eatToken(TokenKind expectedKind) {
		Token t = nextToken();
		if (t==null) {
			raiseInternalException( expressionString.length(), XDDSLMessages.OOD);
		}
		if (t.kind!=expectedKind) {
			raiseInternalException(t.startpos,XDDSLMessages.NOT_EXPECTED_TOKEN, expectedKind.toString().toLowerCase(),t.getKind().toString().toLowerCase());
		}
		return t;
	}

	private boolean peekToken(TokenKind desiredTokenKind) {
		return peekToken(desiredTokenKind,false);
	}

	private boolean lookAhead(int distance,TokenKind desiredTokenKind) {
		if ((tokenStreamPointer+distance)>=tokenStream.size()) {
			return false;
		}
		Token t = tokenStream.get(tokenStreamPointer+distance);
		if (t.kind==desiredTokenKind) {
			return true;
		}
		return false;
	}

	private boolean peekToken(TokenKind desiredTokenKind1,TokenKind desiredTokenKind2) {
		return peekToken(desiredTokenKind1,false) || peekToken(desiredTokenKind2,false);
	}

	private boolean peekToken(TokenKind desiredTokenKind, boolean consumeIfMatched) {
		if (!moreTokens()) {
			return false;
		}
		Token t = peekToken();
		if (t.kind==desiredTokenKind) {
			if (consumeIfMatched) {
				tokenStreamPointer++;
			}
			return true;
		} else {
			return false;
		}
	}

	private boolean moreTokens() {
		return tokenStreamPointer<tokenStream.size();
	}

	private Token nextToken() {
		if (tokenStreamPointer>=tokenStreamLength) {
			return null;
		}
		return tokenStream.get(tokenStreamPointer++);
	}

	private boolean isNextTokenAdjacent() {
		if (tokenStreamPointer>=tokenStreamLength) {
			return false;
		}
		Token last = tokenStream.get(tokenStreamPointer-1);
		Token next = tokenStream.get(tokenStreamPointer);
		return next.startpos==last.endpos;
	}

	private Token peekToken() {
		if (tokenStreamPointer>=tokenStreamLength) {
			return null;
		}
		return tokenStream.get(tokenStreamPointer);
	}

	private void raiseInternalException(int pos, XDDSLMessages message,Object... inserts) {
		throw new InternalParseException(new DSLParseException(expressionString,pos,message,inserts));
	}

	public String toString(Token t) {
		if (t.getKind().hasPayload()) {
			return t.stringValue();
		} else {
			return t.kind.toString().toLowerCase();
		}
	}
}
