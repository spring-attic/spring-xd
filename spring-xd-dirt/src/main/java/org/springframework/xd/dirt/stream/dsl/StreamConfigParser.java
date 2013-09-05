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
import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.BaseDefinition;

/**
 * @author Andy Clement
 */
public class StreamConfigParser implements StreamLookupEnvironment {

	private String expressionString;

	private List<Token> tokenStream;

	private int tokenStreamLength;

	private int tokenStreamPointer; // Current location in the token stream when
									// processing tokens

	/** The repository (if supplied) is used to chase down substream/label references */
	private CrudRepository<? extends BaseDefinition, String> repository;

	public StreamConfigParser(CrudRepository<? extends BaseDefinition, String> repository) {
		this.repository = repository;
	}

	/**
	 * Parse a stream definition without supplying the stream name up front. The stream name may be embedded in the
	 * definition. For example: <code>mystream = http | file</code>
	 * 
	 * @return the AST for the parsed stream
	 */
	public StreamsNode parse(String stream) {
		return parse(null, stream);
	}

	/**
	 * Parse a stream definition. Can throw a DSLException.
	 * 
	 * @return the AST for the parsed stream
	 */
	public StreamsNode parse(String name, String stream) {
		this.expressionString = stream;
		Tokenizer tokenizer = new Tokenizer(expressionString);
		tokenStream = tokenizer.getTokens();
		tokenStreamLength = tokenStream.size();
		tokenStreamPointer = 0;
		StreamsNode ast = eatStreams();
		// Check if the stream name is same as that of any of its modules' names
		// Throw DSLException as allowing this causes the parser to lookup the stream indefinitely.
		for (StreamNode streamNode : ast.getStreamNodes()) {
			if (streamNode.getModule(name) != null) {
				throw new DSLException(stream, stream.indexOf(name), XDDSLMessages.STREAM_NAME_MATCHING_MODULE_NAME,
						name);
			}
		}
		if (moreTokens()) {
			throw new DSLException(this.expressionString, peekToken().startpos, XDDSLMessages.MORE_INPUT,
					toString(nextToken()));
		}
		ast.resolve(this);
		return ast;
	}

	// streams: stream ([;\n] stream)*
	private StreamsNode eatStreams() {
		List<StreamNode> streamNodes = new ArrayList<StreamNode>();
		streamNodes.add(eatStream());
		while (peekTokenAndConsume(TokenKind.NEWLINE, TokenKind.SEMICOLON)) {
			streamNodes.add(eatStream());
		}
		return new StreamsNode(this.expressionString, streamNodes);
	}

	// (name =)
	public String maybeEatStreamName() {
		String streamName = null;
		if (lookAhead(1, TokenKind.EQUALS)) {
			if (peekToken(TokenKind.IDENTIFIER)) {
				streamName = eatToken(TokenKind.IDENTIFIER).data;
				nextToken(); // skip '='
			}
			else {
				raiseException(peekToken().startpos, XDDSLMessages.ILLEGAL_STREAM_NAME, toString(peekToken()));
			}
		}
		return streamName;
	}

	// stream: (streamName) (sourceChannel) moduleList (sinkChannel)
	private StreamNode eatStream() {
		String streamName = maybeEatStreamName();
		SourceChannelNode sourceChannelNode = maybeEatSourceChannel();
		List<ModuleNode> moduleNodes = eatModuleList();
		SinkChannelNode sinkChannelNode = maybeEatSinkChannel();

		// A stream ends with the end of the data or a newline or semicolon
		// Anything else is unexpected data
		if (moreTokens()) {
			Token t = peekToken();
			if (!(t.kind == TokenKind.NEWLINE || t.kind == TokenKind.SEMICOLON)) {
				raiseException(peekToken().startpos, XDDSLMessages.UNEXPECTED_DATA_AFTER_STREAMDEF,
						toString(peekToken()));
			}
		}

		StreamNode streamNode = new StreamNode(streamName, moduleNodes, sourceChannelNode, sinkChannelNode);
		return streamNode;
	}

	// moduleReference: (streamName '.')labelOrModuleName
	public ModuleReferenceNode eatPossiblyQualifiedLabelOrModuleReference() {
		Token firstToken = eatToken(TokenKind.IDENTIFIER);
		if (peekToken(TokenKind.DOT, true)) {
			// firstToken is the qualifying stream name
			Token labelOrModuleToken = eatToken(TokenKind.IDENTIFIER);
			return new ModuleReferenceNode(firstToken.data, labelOrModuleToken.data, firstToken.startpos,
					labelOrModuleToken.endpos);
		}
		else {
			return new ModuleReferenceNode(null, firstToken.data, firstToken.startpos, firstToken.endpos);
		}
	}

	private SourceChannelNode maybeEatSourceChannel() {
		// In order to continue to support the old original style tap, let's do this:
		// TODO remove this block when we don't need the old taps
		boolean looksLikeOldTap = true;
		// Seek for a GT before a PIPE
		for (int tp = tokenStreamPointer; tp < tokenStreamLength; tp++) {
			if (tokenStream.get(tp).getKind() == TokenKind.GT) {
				looksLikeOldTap = false;
				break;
			}
			else if (tokenStream.get(tp).getKind() == TokenKind.PIPE) {
				break;
			}
		}
		if (looksLikeOldTap) {
			return null;
		}

		// The first module encountered might be a source channel module
		// eg. ":foo >"
		SourceChannelNode sourceChannelNode = null;
		boolean isTapToken = false;
		Token t = peekToken();
		if (t.getKind() == TokenKind.IDENTIFIER && t.data.equals("tap")) {
			// tapping source channel
			nextToken();
			isTapToken = true;
			// TODO assert that it is followed by channel reference
		}
		if (peekToken(TokenKind.COLON)) {
			ChannelNode channelNode = eatChannelReference(true);
			Token gt = eatToken(TokenKind.GT);
			sourceChannelNode = new SourceChannelNode(channelNode, gt.endpos, isTapToken);
		}
		else if (isTapToken) {
			// A tap can be followed with an optionally stream qualified label or module
			// reference
			ModuleReferenceNode moduleReferenceNode = eatPossiblyQualifiedLabelOrModuleReference();
			Token gt = eatToken(TokenKind.GT);
			sourceChannelNode = new SourceChannelNode(moduleReferenceNode, gt.endpos, isTapToken);
		}
		return sourceChannelNode;
	}

	private SinkChannelNode maybeEatSinkChannel() {
		// The last part of the stream might be a sink channel
		// eg. "> :foo"
		SinkChannelNode sinkChannelNode = null;
		if (peekToken(TokenKind.GT)) {
			Token gt = eatToken(TokenKind.GT);
			ChannelNode channelNode = eatChannelReference(false);
			sinkChannelNode = new SinkChannelNode(channelNode, gt.startpos);
		}
		return sinkChannelNode;
	}

	// if allowQualifiedChannel expects :(streamName '.')channelName
	// else expects :channelName
	private ChannelNode eatChannelReference(boolean allowQualifiedChannel) {
		Token colon = nextToken();
		if (!colon.isKind(TokenKind.COLON)) {
			raiseException(colon.startpos, XDDSLMessages.EXPECTED_CHANNEL_QUALIFIER, toString(colon));
		}
		Token firstToken = nextToken();

		if (!firstToken.isIdentifier()) {
			raiseException(firstToken.startpos, XDDSLMessages.EXPECTED_CHANNEL_NAME, toString(firstToken));
		}
		// Supporting ":tap:stream" or ":tap:stream.channel"
		if (firstToken.isIdentifier() && firstToken.data.equals("tap") && peekToken(TokenKind.COLON, true)) {
			Token streamName = eatToken(TokenKind.IDENTIFIER);
			Token moduleName = null;
			if (peekToken(TokenKind.DOT, true)) {
				moduleName = eatToken(TokenKind.IDENTIFIER);
			}
			ChannelNode channelNode = new ChannelNode(streamName.data, moduleName == null ? null : moduleName.data,
					colon.startpos,
					moduleName == null ? streamName.endpos : moduleName.endpos);
			channelNode.setIsTap(true);
			return channelNode;
		}
		if (peekToken(TokenKind.COLON, true)) {
			Token suffixToken = eatToken(TokenKind.IDENTIFIER);
			firstToken = new Token(TokenKind.IDENTIFIER, (firstToken.data + ":" + suffixToken.data).toCharArray(),
					firstToken.startpos, suffixToken.endpos);
		}

		if (allowQualifiedChannel && peekToken(TokenKind.DOT, true)) {
			// firstToken is actually a stream name
			Token channelToken = eatToken(TokenKind.IDENTIFIER);
			return new ChannelNode(firstToken.data, channelToken.data, colon.startpos, channelToken.endpos);
		}
		else {
			return new ChannelNode(null, firstToken.data, colon.startpos, firstToken.endpos);
		}
	}

	// moduleList: module (| module)*
	private List<ModuleNode> eatModuleList() {
		List<ModuleNode> moduleNodes = new ArrayList<ModuleNode>();

		moduleNodes.add(eatModule());
		while (moreTokens()) {
			Token t = peekToken();
			if (t.kind == TokenKind.PIPE) {
				nextToken();
				moduleNodes.add(eatModule());
			}
			else if (t.kind == TokenKind.AND) {
				// Defining a sequence of job steps
				nextToken();
				// tag previous node
				ModuleNode lastModule = moduleNodes.get(moduleNodes.size() - 1);
				lastModule.setIsJobStep(true);

				// tag next node
				ModuleNode nextModule = eatModule();
				nextModule.setIsJobStep(true);
				moduleNodes.add(nextModule);
			}
			else {
				// might be followed by sink channel or newline/semicolon to end this
				// stream
				break;
			}
		}
		return moduleNodes;
	}

	// TODO [Andy] temporary to support horrid tap @ syntax
	boolean isTap = false;

	// module: [label':']* identifier (moduleArguments)*
	private ModuleNode eatModule() {
		List<Token> labels = null;
		Token name = nextToken();
		if (!name.isKind(TokenKind.IDENTIFIER)) {
			raiseException(name.startpos, XDDSLMessages.EXPECTED_MODULENAME, name.data != null ? name.data
					: new String(name.getKind().tokenChars));
		}
		while (peekToken(TokenKind.COLON, true)) {
			if (labels == null) {
				labels = new ArrayList<Token>();
			}
			labels.add(name);
			name = eatToken(TokenKind.IDENTIFIER);
		}
		Token moduleName = name;
		isTap = (moduleName.data.equals("tap"));
		ArgumentNode[] args = maybeEatModuleArgs();
		int startpos = moduleName.startpos;
		if (labels != null) {
			startpos = labels.get(0).startpos;
		}
		return new ModuleNode(toLabelNodes(labels), moduleName.data, startpos, moduleName.endpos, args);
	}

	private List<LabelNode> toLabelNodes(List<Token> labels) {
		if (labels == null) {
			return null;
		}
		List<LabelNode> labelNodes = new ArrayList<LabelNode>();
		for (Token label : labels) {
			labelNodes.add(new LabelNode(label.data, label.startpos, label.endpos));
		}
		return labelNodes;
	}

	// moduleArguments : DOUBLE_MINUS identifier(name) EQUALS identifier(value)
	private ArgumentNode[] maybeEatModuleArgs() {
		List<ArgumentNode> args = null;
		if (isTap && !peekToken(TokenKind.REFERENCE)) {
			Token streamtoken = eatToken(TokenKind.IDENTIFIER);
			String streamname = streamtoken.data;
			String modulename = null;
			Token moduletoken = null;
			// There may be a module name specified, otherwise the source
			// of the target stream is implied
			if (peekToken(TokenKind.DOT)) {
				eatToken(TokenKind.DOT);
				moduletoken = eatToken(TokenKind.IDENTIFIER);
				modulename = moduletoken.data;
			}

			// Let's map the modulename
			StreamNode existingAst = lookupStream(streamname);
			if (existingAst == null) {
				raiseException(streamtoken.startpos, XDDSLMessages.UNRECOGNIZED_STREAM_REFERENCE, streamname);
			}

			int indexedModule = -1;
			if (modulename == null) {
				indexedModule = 0;
			}
			else {
				List<ModuleNode> modules = existingAst.getModuleNodes();

				// Must be a module name, find its index
				for (int m = 0; m < modules.size(); m++) {
					if (modules.get(m).getName().equals(modulename)) {
						if (indexedModule != -1) {
							raiseException(moduletoken.startpos, XDDSLMessages.AMBIGUOUS_MODULE_NAME,
									modules.get(m).getName(), streamname, indexedModule, m);
						}
						indexedModule = m;
					}
					List<String> labels = modules.get(m).getLabelNames();
					for (String label : labels) {
						if (label.equals(modulename)) {
							if (indexedModule != -1) {
								raiseException(moduletoken.startpos, XDDSLMessages.AMBIGUOUS_MODULE_NAME, label,
										streamname, indexedModule, m);
							}
							indexedModule = m;
						}
					}
				}
				if (indexedModule == -1) {
					try {
						indexedModule = Integer.parseInt(modulename);
					}
					catch (NumberFormatException nfe) {
						// let us fail later with the unrecognized reference message
					}
				}
			}
			if (indexedModule != -1) {
				args = new ArrayList<ArgumentNode>();
				String argValue = null;
				if (modulename != null && indexedModule == existingAst.getModuleNodes().size() - 1) {
					// If the selected stream is using a sink channel the tap needs to
					// be on the named channel, we cannot assume stream.NNN
					SinkChannelNode sinkChannelNode = existingAst.getSinkChannelNode();
					if (sinkChannelNode != null) {
						argValue = sinkChannelNode.getChannelName();
					}
				}
				else {
					argValue = streamname + "." + indexedModule;
				}
				args.add(new ArgumentNode("channel", argValue, streamtoken.startpos,
						(moduletoken == null ? streamtoken.endpos : moduletoken.endpos)));
			}
			else {
				raiseException(moduletoken.startpos, XDDSLMessages.UNRECOGNIZED_MODULE_REFERENCE, modulename);
			}
			return args.toArray(new ArgumentNode[args.size()]);
		}

		if (peekToken(TokenKind.DOUBLE_MINUS) && isNextTokenAdjacent()) {
			raiseException(peekToken().startpos, XDDSLMessages.EXPECTED_WHITESPACE_AFTER_MODULE_BEFORE_ARGUMENT);
		}
		while (peekToken(TokenKind.DOUBLE_MINUS, TokenKind.REFERENCE)) {
			Token optionQualifier = nextToken(); // skip the '--' (or '@' at the
													// moment...)

			// This is dirty, temporary, until we nail the tap syntax
			if (isTap) {
				if (optionQualifier.getKind() == TokenKind.REFERENCE) {
					Token t = peekToken();
					String argValue = eatArgValue();
					if (peekToken(TokenKind.DOT)) {
						// tap @foo.NNN
						nextToken();
						String channelNumber = eatToken(TokenKind.IDENTIFIER).data;
						argValue = argValue + "." + channelNumber;
					}
					else {
						// no channel number specified
						argValue = argValue + ".0";
					}

					if (args == null) {
						args = new ArrayList<ArgumentNode>();
					}
					args.add(new ArgumentNode("channel", argValue, optionQualifier.startpos, t.endpos));
					continue;
				}
			}
			if (peekToken(TokenKind.IDENTIFIER) && !isNextTokenAdjacent()) {
				raiseException(peekToken().startpos, XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_NAME);
			}
			Token argName = eatToken(TokenKind.IDENTIFIER);
			if (peekToken(TokenKind.EQUALS) && !isNextTokenAdjacent()) {
				raiseException(peekToken().startpos, XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_EQUALS);
			}
			eatToken(TokenKind.EQUALS);
			if (peekToken(TokenKind.IDENTIFIER) && !isNextTokenAdjacent()) {
				raiseException(peekToken().startpos, XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_VALUE);
			}
			// Process argument value:
			Token t = peekToken();
			// if (t==null) {
			// raiseException(this.expressionString.length()-1,XDDSLMessages.OOD));
			// }
			String argValue = eatArgValue();

			if (args == null) {
				args = new ArrayList<ArgumentNode>();
			}
			args.add(new ArgumentNode(argName.data, argValue, argName.startpos - 2, t.endpos));
		}
		return args == null ? null : args.toArray(new ArgumentNode[args.size()]);
	}

	// argValue: identifier | literal_string
	private String eatArgValue() {
		Token t = nextToken();
		String argValue = null;
		if (t.getKind() == TokenKind.IDENTIFIER) {
			argValue = t.data;
		}
		else if (t.getKind() == TokenKind.LITERAL_STRING) {
			argValue = t.data.substring(1, t.data.length() - 1).replaceAll("''", "'").replaceAll("\"\"", "\"");
		}
		else {
			raiseException(t.startpos, XDDSLMessages.EXPECTED_ARGUMENT_VALUE, t.data);
		}
		return argValue;
	}

	private Token eatToken(TokenKind expectedKind) {
		Token t = nextToken();
		if (t == null) {
			raiseException(expressionString.length(), XDDSLMessages.OOD);
		}
		if (t.kind != expectedKind) {
			raiseException(t.startpos, XDDSLMessages.NOT_EXPECTED_TOKEN, expectedKind.toString().toLowerCase(),
					t.getKind().toString().toLowerCase() + (t.data == null ? "" : "(" + t.data + ")"));
		}
		return t;
	}

	private boolean peekToken(TokenKind desiredTokenKind) {
		return peekToken(desiredTokenKind, false);
	}

	private boolean lookAhead(int distance, TokenKind desiredTokenKind) {
		if ((tokenStreamPointer + distance) >= tokenStream.size()) {
			return false;
		}
		Token t = tokenStream.get(tokenStreamPointer + distance);
		if (t.kind == desiredTokenKind) {
			return true;
		}
		return false;
	}

	private boolean peekToken(TokenKind desiredTokenKind1, TokenKind desiredTokenKind2) {
		return peekToken(desiredTokenKind1, false) || peekToken(desiredTokenKind2, false);
	}

	private boolean peekTokenAndConsume(TokenKind desiredTokenKind1, TokenKind desiredTokenKind2) {
		return peekToken(desiredTokenKind1, true) || peekToken(desiredTokenKind2, true);
	}

	private boolean peekToken(TokenKind desiredTokenKind, boolean consumeIfMatched) {
		if (!moreTokens()) {
			return false;
		}
		Token t = peekToken();
		if (t.kind == desiredTokenKind) {
			if (consumeIfMatched) {
				tokenStreamPointer++;
			}
			return true;
		}
		else {
			return false;
		}
	}

	private boolean moreTokens() {
		return tokenStreamPointer < tokenStream.size();
	}

	private Token nextToken() {
		if (tokenStreamPointer >= tokenStreamLength) {
			raiseException(expressionString.length(), XDDSLMessages.OOD);
		}
		return tokenStream.get(tokenStreamPointer++);
	}

	private boolean isNextTokenAdjacent() {
		if (tokenStreamPointer >= tokenStreamLength) {
			return false;
		}
		Token last = tokenStream.get(tokenStreamPointer - 1);
		Token next = tokenStream.get(tokenStreamPointer);
		return next.startpos == last.endpos;
	}

	private Token peekToken() {
		if (tokenStreamPointer >= tokenStreamLength) {
			return null;
		}
		return tokenStream.get(tokenStreamPointer);
	}

	private void raiseException(int pos, XDDSLMessages message, Object... inserts) {
		throw new DSLException(expressionString, pos, message, inserts);
	}

	public String toString(Token t) {
		if (t.getKind().hasPayload()) {
			return t.stringValue();
		}
		else {
			return new String(t.kind.getTokenChars());
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(tokenStream).append("\n");
		s.append("tokenStreamPointer=" + tokenStreamPointer).append("\n");
		return s.toString();
	}

	// LookupEnvironment implementation

	@Override
	public StreamNode lookupStream(String name) {
		if (this.repository != null) {
			BaseDefinition baseDefinition = repository.findOne(name);
			if (baseDefinition != null) {
				StreamsNode streamsNode = new StreamConfigParser(repository).parse(baseDefinition.getDefinition());
				Assert.isTrue(streamsNode.getSize() == 1);
				return streamsNode.getStreamNodes().get(0);
			}
		}
		return null;
	}

	// Expects channel naming scheme of stream.NNN where NNN is the module index in the
	// stream
	@Override
	public String lookupChannelForLabelOrModule(String streamName, String streamOrLabelOrModuleName) {
		if (streamName != null) {
			BaseDefinition basedef = repository.findOne(streamName);
			if (basedef == null) {
				// TODO error/warning?
				return null;
			}
			StreamsNode streamsNode = new StreamConfigParser(repository).parse(basedef.getDefinition());
			if (streamsNode != null && streamsNode.getSize() == 1) {
				int index = streamsNode.getStreamNodes().get(0).getIndexOfLabelOrModuleName(streamOrLabelOrModuleName);
				if (index == -1) {
					// TODO could be an error
					return streamName + "." + 0;
				}
				else {
					return streamName + "." + index;
				}
			}
		}
		else {
			// Is it a stream?
			BaseDefinition basedef = repository.findOne(streamOrLabelOrModuleName);
			if (basedef != null) {
				return streamOrLabelOrModuleName + ".0";
			}
			// look through all streams...
			for (BaseDefinition bd : repository.findAll()) {
				StreamsNode streamsNode = new StreamConfigParser(repository).parse(bd.getDefinition());
				StreamNode sn = streamsNode.getStreamNodes().get(0);
				int index = sn.getIndexOfLabelOrModuleName(streamOrLabelOrModuleName);
				if (index != -1) {
					return sn.getStreamName() + "." + index;
				}
			}
		}
		return null;
	}
}
