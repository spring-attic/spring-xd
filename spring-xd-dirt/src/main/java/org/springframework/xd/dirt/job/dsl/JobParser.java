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

import java.util.ArrayList;
import java.util.List;

/**
 * Parse a Batch DSL Job specification.
 *
 * @author Andy Clement
 */
public class JobParser {

	// If true then inline job definitions will be allowed in the DSL.
	final static boolean SUPPORTS_INLINE_JOB_DEFINITIONS = false;

	private Tokens tokens;

	public JobParser() {
	}

	/**
	 * Parse a job flow definition into an abstract syntax tree (AST).
	 *
	 * @param jobSpecification the textual job specification
	 * @return the AST for the parsed job
	 * @throws JobSpecificationException if any problems occur during parsing
	 */
	public JobSpecification parse(String jobSpecification) {
		jobSpecification = jobSpecification.trim();
		if (jobSpecification.length() == 0) {
			return new JobSpecification(jobSpecification, null, null);
		}
		tokens = new Tokens(jobSpecification);
		JobNode jobNode = parseJobNode();
		ArgumentNode[] globalOptions = maybeEatModuleArgs();
		JobSpecification js = new JobSpecification(jobSpecification, jobNode, globalOptions);
		if (tokens.hasNext()) {
			throw new JobSpecificationException(tokens.getExpression(), tokens.peek().startpos,
					JobDSLMessage.UNEXPECTED_DATA_AFTER_JOBSPEC,
					toString(tokens.next()));
		}
		return js;
	}


	private JobNode parseJobNode() {
		// Handle (...)
		if (tokens.maybeEat(TokenKind.OPEN_PAREN)) {
			JobNode jn = parseJobNode();
			tokens.eat(TokenKind.CLOSE_PAREN);
			return jn;
		}
		// Handle a split < ... >
		if (tokens.peek(TokenKind.SPLIT_OPEN)) {
			JobNode jn = parseSplit();
			// is the split part of a flow? "<..> || b"
			return maybeParseFlow(jn);
		}
		JobDescriptor jd = parseJobDescriptor();
		// Handle a potential flow "a || b"
		return maybeParseFlow(jd);
	}

	private JobNode maybeParseFlow(JobNode firstJobNodeInFlow) {
		if (tokens.peek(TokenKind.DOUBLE_PIPE)) {
			List<JobNode> jobNodes = new ArrayList<>();
			jobNodes.add(firstJobNodeInFlow);
			while (tokens.maybeEat(TokenKind.DOUBLE_PIPE)) {
				JobNode nextNode = parseJobNode();
				// If nextNode is a Flow node, merge it with this one
				if (nextNode instanceof Flow) {
					jobNodes.addAll(nextNode.getSeries());
				}
				else {
					jobNodes.add(nextNode);
				}
			}
			return new Flow(jobNodes);
		}
		else {
			return firstJobNodeInFlow;
		}
	}

	// '<' jobs ['&' jobs]+ '>'
	private JobNode parseSplit() {
		List<JobNode> flows = new ArrayList<>();
		tokens.eat(TokenKind.SPLIT_OPEN);
		// '<' jobSequence  [ '&' jobSequence]* '>'
		flows.add(parseJobNode());
		while (tokens.maybeEat(TokenKind.AMPERSAND)) {
			flows.add(parseJobNode());
		}
		tokens.eat(TokenKind.SPLIT_CLOSE);
		return new Split(flows);
	}

	// JobDescriptor can be a simple JobReference (reference to a pre-existing definition) or
	// an inlined JobDefinition.
	//
	// JobReference := identifier:jobname ['--'optionname'='optionvalue']* [stateTransitions]*
	// JobDefinition := identifier:jobmodulename identifier:jobname ['--'optionname'='optionvalue]* [stateTransitions]*
	private JobDescriptor parseJobDescriptor() {
		JobDescriptor jd = null;
		// Double identifiers indicates job definition
		if (tokens.peek(0, TokenKind.IDENTIFIER)) {
			if (tokens.peek(1, TokenKind.IDENTIFIER)) {
				if (SUPPORTS_INLINE_JOB_DEFINITIONS) {
					jd = parseJobDefinition();
				}
				else {
					tokens.raiseException(tokens.peek(1).startpos, JobDSLMessage.EXPECTED_FLOW_OR_SPLIT_CHARS,
							tokens.peek(1).stringValue());
				}
			}
			else {
				jd = parseJobReference(false);
			}
		}
		if (jd == null) {
			Token t = tokens.peek();
			tokens.raiseException(t == null ? 0 : t.startpos, JobDSLMessage.EXPECTED_JOB_REF_OR_DEF);
		}
		if (tokens.peek(TokenKind.PIPE)) {
			List<Transition> transitions = parseTransitions();
			jd.setTransitions(transitions);
		}
		return jd;
	}

	// [| state = target]*
	// where state is an identifier/string literal whilst target is an identifier (target job).
	private List<Transition> parseTransitions() {
		List<Transition> transitions = new ArrayList<>();
		while (tokens.maybeEat(TokenKind.PIPE)) {
			Token transitionName = tokens.next();
			if (!transitionName.isKind(TokenKind.IDENTIFIER) && !transitionName.isKind(TokenKind.LITERAL_STRING)) {
				tokens.raiseException(transitionName.startpos, JobDSLMessage.EXPECTED_TRANSITION_NAME,
						tokenToString(transitionName));
			}
			Token nextToken = tokens.peek();
			if (nextToken == null) {
				// no equals after the transition name
				tokens.raiseException(transitionName.endpos - 1, JobDSLMessage.MISSING_EQUALS_AFTER_TRANSITION_NAME,
						tokenToString(transitionName));
			}
			if (!nextToken.isKind(TokenKind.EQUALS)) {
				tokens.raiseException(nextToken.startpos, JobDSLMessage.EXPECTED_EQUALS_AFTER_TRANSITION_NAME,
						tokenToString(nextToken));
			}
			else {
				tokens.next();
			}
			JobReference targetJobReference = parseJobReference(false);
			transitions.add(new Transition(transitionName, targetJobReference));
		}
		return transitions;
	}

	private String tokenToString(Token token) {
		return token.stringValue() == null ? new String(token.kind.tokenChars)
				: token.stringValue();
	}

	private JobReference parseJobReference(boolean allowArguments) {
		Token jobDefinitionNameToken = tokens.eat(TokenKind.IDENTIFIER);
		tokens.checkpoint();
		ArgumentNode[] args = null;
		if (allowArguments) {
			args = maybeEatModuleArgs();
		}
		// Not currently policing this
		//		else {
		//			if (tokens.peek(TokenKind.DOUBLE_MINUS)) {
		//				tokens.raiseException(tokens.peek().startpos,
		//						JobDSLMessage.JOB_REF_DOES_NOT_SUPPORT_OPTIONS, jobDefinitionNameToken.data);
		//			}
		//		}
		return new JobReference(jobDefinitionNameToken, args);
	}

	private JobDefinition parseJobDefinition() {
		Token jobModuleNameToken = tokens.eat(TokenKind.IDENTIFIER);
		Token jobName = tokens.eat(TokenKind.IDENTIFIER);
		tokens.checkpoint();
		ArgumentNode[] args = maybeEatModuleArgs();
		return new JobDefinition(jobModuleNameToken, jobName, args);
	}

	// ['--'identifier:name'='identifier:value]*
	private ArgumentNode[] maybeEatModuleArgs() {
		List<ArgumentNode> args = null;
		if (tokens.peek(TokenKind.DOUBLE_MINUS) && tokens.isNextAdjacent()) {
			tokens.raiseException(tokens.peek().startpos, JobDSLMessage.EXPECTED_WHITESPACE_AFTER_NAME_BEFORE_ARGUMENT);
		}
		while (tokens.peek(TokenKind.DOUBLE_MINUS)) {
			Token dashDash = tokens.next(); // skip the '--'
			if (tokens.peek(TokenKind.IDENTIFIER) && !tokens.isNextAdjacent()) {
				tokens.raiseException(tokens.peek().startpos, JobDSLMessage.NO_WHITESPACE_BEFORE_ARG_NAME);
			}
			Token argName = tokens.next();
			if (!argName.isKind(TokenKind.IDENTIFIER)) {
				tokens.raiseException(argName.startpos, JobDSLMessage.NOT_EXPECTED_TOKEN,
						argName.data != null ? argName.data
								: new String(argName.getKind().tokenChars));
			}
			if (tokens.peek(TokenKind.EQUALS) && !tokens.isNextAdjacent()) {
				tokens.raiseException(tokens.peek().startpos, JobDSLMessage.NO_WHITESPACE_BEFORE_ARG_EQUALS);
			}
			tokens.eat(TokenKind.EQUALS);
			if (tokens.peek(TokenKind.IDENTIFIER) && !tokens.isNextAdjacent()) {
				tokens.raiseException(tokens.peek().startpos, JobDSLMessage.NO_WHITESPACE_BEFORE_ARG_VALUE);
			}
			// Process argument value:
			Token t = tokens.peek();
			String argValue = eatArgValue();
			tokens.checkpoint();
			if (args == null) {
				args = new ArrayList<ArgumentNode>();
			}
			args.add(new ArgumentNode(argName.data, argValue, dashDash.startpos, t.endpos));
		}
		return args == null ? null : args.toArray(new ArgumentNode[args.size()]);
	}

	// [identifier | literal_string]
	private String eatArgValue() {
		Token t = tokens.next();
		String argValue = null;
		if (t.getKind() == TokenKind.IDENTIFIER) {
			argValue = t.data;
		}
		else if (t.getKind() == TokenKind.LITERAL_STRING) {
			String quotesUsed = t.data.substring(0, 1);
			argValue = t.data.substring(1, t.data.length() - 1).replace(quotesUsed + quotesUsed, quotesUsed);
		}
		else {
			tokens.raiseException(t.startpos, JobDSLMessage.EXPECTED_ARGUMENT_VALUE, t.data);
		}
		return argValue;
	}

	/**
	 * Convert a token into its String representation.
	 *
	 * @param t the token
	 * @return a string representation of the supplied token
	 */
	private static String toString(Token t) {
		if (t.getKind().hasPayload()) {
			return t.stringValue();
		}
		else {
			return new String(t.kind.getTokenChars());
		}
	}

	/**
	 * Create a graph suitable for Flo to display.
	 *
	 * @param jobSpecification the textual definition of the specification
	 * @return a Graph representation of the supplied specification
	 */
	public Graph getGraph(String jobSpecification) {
		JobSpecification js = parse(jobSpecification);
		return js.toGraph();
	}

	/**
	 * Create an XML representation of the JobSpec.
	 *
	 * @param batchJobId the id that will be inserted into the XML document for the batch:job element
	 * @param jobSpecification the textual definition of the specification
	 * @return a String containing XML representing the supplied specification
	 */
	public String getXML(String batchJobId, String jobSpecification) {
		JobSpecification js = parse(jobSpecification);
		return js.toXML(batchJobId);
	}

	/**
	 * Show the parsing progress in the output string.
	 */
	@Override
	public String toString() {
		// Only state of the token processing is interesting:
		return tokens.toString();
	}
}
