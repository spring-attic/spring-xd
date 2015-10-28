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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Parse job specifications and verify either the correct abstract syntax tree is produced or the current exception comes out.
 * The parser does no semantic validation, it is purely syntax checking.
 *
 * @author Andy Clement
 */
public class JobParserTests {

	private JobSpecification js;

	@Test
	public void jobTokenization() {
		Tokens tokens = new Tokens("jobA | BROKEN = $END |'*'=something");
		assertToken(TokenKind.IDENTIFIER, "jobA", 0, 4, tokens.next());
		assertToken(TokenKind.PIPE, "|", 5, 6, tokens.next());
		assertToken(TokenKind.IDENTIFIER, "BROKEN", 7, 13, tokens.next());
		assertToken(TokenKind.EQUALS, "=", 14, 15, tokens.next());
		assertToken(TokenKind.IDENTIFIER, "$END", 16, 20, tokens.next());
		assertToken(TokenKind.PIPE, "|", 21, 22, tokens.next());
		assertToken(TokenKind.LITERAL_STRING, "'*'", 22, 25, tokens.next());
		assertToken(TokenKind.EQUALS, "=", 25, 26, tokens.next());
		assertToken(TokenKind.IDENTIFIER, "something", 26, 35, tokens.next());
	}

	@Test
	public void rogueTokenization() {
		Tokens tokens = new Tokens("<(xx | '*'=$END) & yy>");
		assertToken(TokenKind.SPLIT_OPEN, "<", 0, 1, tokens.next());
		assertToken(TokenKind.OPEN_PAREN, "(", 1, 2, tokens.next());
		assertToken(TokenKind.IDENTIFIER, "xx", 2, 4, tokens.next());
		assertToken(TokenKind.PIPE, "|", 5, 6, tokens.next());
		assertToken(TokenKind.LITERAL_STRING, "'*'", 7, 10, tokens.next());
		assertToken(TokenKind.EQUALS, "=", 10, 11, tokens.next());
		assertToken(TokenKind.IDENTIFIER, "$END", 11, 15, tokens.next());
		assertToken(TokenKind.CLOSE_PAREN, ")", 15, 16, tokens.next());
		assertToken(TokenKind.AMPERSAND, "&", 17, 18, tokens.next());
		assertToken(TokenKind.IDENTIFIER, "yy", 19, 21, tokens.next());
		assertToken(TokenKind.SPLIT_CLOSE, ">", 21, 22, tokens.next());
	}

	@Test
	public void rogueParsing() {
		js = parse("<(xx | '*'=$END) & yy>");
		assertEquals("<xx | '*' = $END & yy>", js.stringify());
	}

	@Test
	public void jobTokenization2() {
		Tokens tokens = new Tokens("jobA|BROKEN=$END|'*'=something");
		assertToken(TokenKind.IDENTIFIER, "jobA", 0, 4, tokens.next());
		assertToken(TokenKind.PIPE, "|", 4, 5, tokens.next());
		assertToken(TokenKind.IDENTIFIER, "BROKEN", 5, 11, tokens.next());
		assertToken(TokenKind.EQUALS, "=", 11, 12, tokens.next());
		assertToken(TokenKind.IDENTIFIER, "$END", 12, 16, tokens.next());
		assertToken(TokenKind.PIPE, "|", 16, 17, tokens.next());
		assertToken(TokenKind.LITERAL_STRING, "'*'", 17, 20, tokens.next());
		assertToken(TokenKind.EQUALS, "=", 20, 21, tokens.next());
		assertToken(TokenKind.IDENTIFIER, "something", 21, 30, tokens.next());
	}

	@Test
	public void globalOptions() {
		js = parse("foo --timeout=100 --pollInterval=1000");
		assertEquals("foo --timeout=100 --pollInterval=1000", js.stringify());
		Map<String, String> options = js.getGlobalOptionsMap();
		assertTrue(options.containsKey("timeout"));
		assertEquals(Integer.toString(100), options.get("timeout"));
		assertTrue(options.containsKey("pollInterval"));
		assertEquals(Integer.toString(1000), options.get("pollInterval"));
		assertEquals(2, options.size());
	}

	@Test
	public void graphToText() {
		checkDSLToGraphAndBackToDSL("foojob");
		checkDSLToGraphAndBackToDSL("<aa | xx = foo & bb>");
	}

	@Test
	public void graphToTextWithAllExitsCoveredNode() {
		//		checkDSLToGraphAndBackToDSL("aaa | FOO = XXX | B = bbb | '*' = ccc || bbb || ccc");
		js = parse("aaa | FOO = XXX | B = bbb | '*' = ccc || bbb || ccc");
		Graph g = js.toGraph();
		System.out.println(g);
		assertEquals("aaa | FOO = XXX | B = bbb | '*' = ccc || bbb || ccc", g.toDSLText());
		//		checkDSLToGraphAndBackToDSL("aaa | '*' = ccc | S1 = bbb || bbb || ccc");
	}

	@Test
	public void graphToTextFlow() {
		checkDSLToGraphAndBackToDSL("foojob || barjob");
	}

	@Test
	public void globalOptionsGraphConversion() {
		checkDSLToGraphAndBackToDSL("foojob || barjob --foo=bar --goo=boo");
	}

	@Test
	public void testMissingPunctuation() {
		// If JobParser.SUPPORTS_INLINE_JOB_DEFINITIONS is set to true, this test will need another branch
		// that deals with that since 'eee fff' is a valid inline job spec
		checkForParseError("<aaa & bbb & ccc> || <ddd & eee fff>", JobDSLMessage.EXPECTED_FLOW_OR_SPLIT_CHARS, 32,
				"fff");
	}

	@Test
	public void referenceWithOptions() {
		js = parse("jobModuleA --foo=bar --boo=gar");
		JobNode jn = js.getJobNode();
		assertTrue(jn.isJobDescriptor());
		JobReference jr = (JobReference) jn;
		assertTrue(jr.isReference());
		assertFalse(jr.isDefinition());
		assertEquals("jobModuleA", jr.getName());
		ArgumentNode[] args = jr.getArguments();
		assertNull(args);
		assertEquals(2, js.getGlobalOptionsMap().size());
	}

	@Test
	public void referenceWithOptionsInTransition() {
		checkForParseError("aaa || bbb | d = foo --aaa=bbb || ccc", JobDSLMessage.UNEXPECTED_DATA_AFTER_JOBSPEC,
				31, "||");
	}

	@Test
	public void graphToTextSplit() {
		checkDSLToGraphAndBackToDSL("<foojob & barjob>");
	}

	@Test
	public void errorMissingJob() {
		// Only single & between jobs
		checkForParseError("<aa && bb>", JobDSLMessage.EXPECTED_JOB_REF_OR_DEF, 5);
	}

	@Test
	public void errorMissingJob2() {
		// Need a job in between all those things
		checkForParseError("aa  |||| bb", JobDSLMessage.EXPECTED_JOB_REF_OR_DEF, 6);
	}

	@Test
	public void errorMissingJob3() {
		// Need a job in between all those things
		checkForParseError("aa  ||| bb", JobDSLMessage.EXPECTED_JOB_REF_OR_DEF, 6);
	}

	@Test
	public void graphToTextFlowWithTransition() {
		checkDSLToGraphAndBackToDSL("foojob | completed = killjob || barjob");
	}

	@Test
	public void graphToTextSplitWithTransition() {
		checkDSLToGraphAndBackToDSL("<foojob | completed = killjob & barjob>");
	}

	@Test
	public void graphToTextSplitWithTransition2() {
		checkDSLToGraphAndBackToDSL("<foojob | completed = killjob & barjob | completed = killjob>");
	}

	@Test
	public void graphToTextComplex() {
		checkDSLToGraphAndBackToDSL("<foojob & bbb || ccc>");
	}

	@Ignore
	@Test
	public void toDSLTextTransitions() {
		js = parse("aaa | '*' = $END || bbb");
		assertEquals("", js.stringify());
		// Unclear if this is valid, there isn't really any orchestration going on, what drives bbb?
		checkDSLToGraphAndBackToDSL("aaa | '*' = $END || bbb");
	}

	@Ignore
	@Test
	// You can't draw this on the graph, it would end up looking like "aaa | '*' = $END || bbb || ccc
	public void toDSLTextTransitionsSplit() {
		checkDSLToGraphAndBackToDSL("aaa | '*' = $END || <bbb & ccc>");
	}

	@Test
	public void toDSLTextTransitionsFlow() {
		checkDSLToGraphAndBackToDSL("aaa | '*' = $END || bbb || ccc");
	}

	@Test
	public void toDSLTextSplitToFlow() {
		checkDSLToGraphAndBackToDSL("<a & b> || foo");
	}

	@Test
	public void toDSLTextWithPropertiesOnDefinition() {
		if (JobParser.SUPPORTS_INLINE_JOB_DEFINITIONS) {
			checkDSLToGraphAndBackToDSL("<a x --aaa=bbb & b> || foo");
			checkDSLToGraphAndBackToDSL("<a & b y --aaa=bbb --ccc=ddd> || foo");
		}
		else {
			checkForParseError("<a x --aaa=bbb & b> || foo", JobDSLMessage.EXPECTED_FLOW_OR_SPLIT_CHARS, 3, "x");
			checkForParseError("<a & b y --aaa=bbb --ccc=ddd> || foo", JobDSLMessage.EXPECTED_FLOW_OR_SPLIT_CHARS, 7,
					"y");
		}
	}

	@Test
	public void toDSLTextWithGlobalOptions() {
		checkDSLToGraphAndBackToDSL("<a & b> || foo --aaa=bbb");
		checkDSLToGraphAndBackToDSL("<a & b> || foo --aaa=bbb --ccc=ddd");
	}

	@Test
	public void toDSLTextFlowToSplit() {
		checkDSLToGraphAndBackToDSL("foo || <c & d>");
	}

	@Test
	public void toDSLTextSplitFlowSplit() {
		checkDSLToGraphAndBackToDSL("<a & b> || foo || <c & d>");
		checkDSLToGraphAndBackToDSL("<a & b> || foo | wibble = $END || <c & d>");
	}

	@Test
	public void toDSLTextFlowTransitions() {
		checkDSLToGraphAndBackToDSL("aaa | COMPLETED = kill || bbb || ccc");
		checkDSLToGraphAndBackToDSL("aaa | COMPLETED = kill || bbb | COMPLETED = kill || ccc");
		checkDSLToGraphAndBackToDSL("aaa | COMPLETED = kill | FOO = bar || bbb | COMPLETED = kill || ccc");
	}

	@Test
	public void toDSLTextSplitTransitions() {
		checkDSLToGraphAndBackToDSL("<aaa | COMPLETED = kill & bbb> || ccc");
		checkDSLToGraphAndBackToDSL("<aaa | COMPLETED = kill & bbb | COMPLETED = kill> || ccc");
		//		checkDSLToGraphAndBackToDSL("<aaa | COMPLETED = kill | '*' = kill2 & bbb | COMPLETED = kill> || ccc");
	}

	@Test
	public void toDSLTextNestedSplits() {
		checkDSLToGraphAndBackToDSL("<aaa & ccc & ddd> || eee");
		checkDSLToGraphAndBackToDSL("<aaa & bbb || <ccc & ddd>> || eee");
		checkDSLToGraphAndBackToDSL("<aaa || <bbb & ccc> || foo & ddd || eee> || fff");
		checkDSLToGraphAndBackToDSL("<aaa || <bbb & ccc> & ddd || eee> || fff");
		checkDSLToGraphAndBackToDSL("<aaa || <bbb & ccc> & ddd || eee> || fff");
		checkDSLToGraphAndBackToDSL("<aaa & bbb || <ccc & ddd>> || <eee & fff>");
		checkDSLToGraphAndBackToDSL("<aaa & bbb || <ccc & ddd>> || <eee & fff> || <ggg & hhh>");
	}

	@Test
	public void toDSLTextLong() {
		checkDSLToGraphAndBackToDSL(
				"<aaa || fff & bbb || ggg || <ccc & ddd>> || eee || hhh || iii || <jjj & kkk || lll>");
	}

	@Test
	public void toDSLTextSync() {
		String spec = "<a & b> || <c & d>";
		checkDSLToGraphAndBackToDSL(spec);
	}

	@Test
	public void toDSLTextSqoop() {
		String spec = "<(sqoop-6e44 | 'FAILED' = kill1" +
				"  || sqoop-e07a | 'FAILED' = kill1) & " +
				" (sqoop-035f | 'FAILED' = kill2" +
				"  || sqoop-9408 | 'FAILED' = kill2" +
				"  || sqoop-a6e0 | 'FAILED' = kill2" +
				"  || sqoop-e522 | 'FAILED' = kill2" +
				"  || shell-b521 | 'FAILED' = kill2) & " +
				" (sqoop-6420 | 'FAILED' = kill3)>";
		// DSL text right now doesn't include parentheses (they aren't necessary)
		spec = "<sqoop-6e44 | 'FAILED' = kill1" +
				" || sqoop-e07a | 'FAILED' = kill1 &" +
				" sqoop-035f | 'FAILED' = kill2" +
				" || sqoop-9408 | 'FAILED' = kill2" +
				" || sqoop-a6e0 | 'FAILED' = kill2" +
				" || sqoop-e522 | 'FAILED' = kill2" +
				" || shell-b521 | 'FAILED' = kill2 &" +
				" sqoop-6420 | 'FAILED' = kill3>";
		checkDSLToGraphAndBackToDSL(spec);
	}

	@Test
	public void toDSLTextManualSync() {
		// Here foo is effectively acting as a sync node
		String spec = "<a & b> || foo || <c & d>";
		checkDSLToGraphAndBackToDSL(spec);
	}

	@Test
	public void oneJobReference() {
		js = parse("foojob");

		// Basic data:
		assertEquals("foojob", js.getJobDefinitionText());
		assertEquals(0, js.getStartPos());
		assertEquals(6, js.getEndPos());

		// The job itself:
		assertEquals("foojob", js.stringify());
		JobNode jn = js.getJobNode();
		assertFalse(jn.isSplit());
		assertFalse(jn.isFlow());

		JobReference jd = (JobReference) jn;
		assertTrue(jd.isReference());
		assertEquals("foojob", jd.getName());
	}

	@Test
	public void whitespace() {
		js = parse("A||B");
		assertEquals("A || B", js.stringify());
		js = parse("<A&B>");
		assertEquals("<A & B>", js.stringify());
		js = parse("<A||B&C>");
		assertEquals("<A || B & C>", js.stringify());
	}

	@Test
	public void endTransition() {
		js = parse("jobA | BROKEN=$END");
		assertEquals("jobA | BROKEN = $END", js.stringify());
		js = parse("jobA|BROKEN=$END");
		assertEquals("jobA | BROKEN = $END", js.stringify());
		js = parse("jobA |BROKEN =$END");
		assertEquals("jobA | BROKEN = $END", js.stringify());
		js = parse("jobA |BROKEN= $END");
		assertEquals("jobA | BROKEN = $END", js.stringify());
	}

	@Test
	public void failTransition() {
		js = parse("jobA | BROKEN=$FAIL");
		assertEquals("jobA | BROKEN = $FAIL", js.stringify());
		js = parse("jobA|BROKEN=$FAIL");
		assertEquals("jobA | BROKEN = $FAIL", js.stringify());
		js = parse("jobA |BROKEN =$FAIL");
		assertEquals("jobA | BROKEN = $FAIL", js.stringify());
		js = parse("jobA |BROKEN= $FAIL");
		assertEquals("jobA | BROKEN = $FAIL", js.stringify());
	}

	@Test
	public void simpleJobSequence() {
		js = parse("jobA || jobB");
		assertEquals("jobA || jobB", js.getJobDefinitionText());
		assertEquals("jobA || jobB", js.stringify());
		JobNode jn = js.getJobNode();
		assertFalse(jn.isSplit());
		assertTrue(jn.isFlow());
		assertEquals(2, jn.getSeriesLength());
		JobNode j1 = jn.getSeriesElement(0);
		JobNode j2 = jn.getSeriesElement(1);
		assertEquals("jobA[0>4]", j1.stringify(true));
		assertEquals("jobB[8>12]", j2.stringify(true));
	}

	@Test
	public void tripleJobSequence() {
		js = parse("jobA || jobB || jobC");
		assertEquals("jobA || jobB || jobC", js.getJobDefinitionText());
		assertEquals("jobA || jobB || jobC", js.stringify());
		JobNode jn = js.getJobNode();
		assertTrue(jn.isFlow());
		assertEquals(3, jn.getSeriesLength());
		JobNode j1 = jn.getSeriesElement(0);
		JobNode j2 = jn.getSeriesElement(1);
		JobNode j3 = jn.getSeriesElement(2);
		assertEquals("jobA[0>4]", j1.stringify(true));
		assertEquals("jobB[8>12]", j2.stringify(true));
		assertEquals("jobC[16>20]", j3.stringify(true));
	}

	@Test
	public void parentheses() {
		js = parse("jobA || (jobB || jobC)");
		assertEquals("jobA || (jobB || jobC)", js.getJobDefinitionText());
		assertEquals("jobA || jobB || jobC", js.stringify());
		JobNode jn = js.getJobNode();
		assertTrue(jn.isFlow());
		assertEquals(3, jn.getSeriesLength());
		JobNode j1 = jn.getSeriesElement(0);
		JobNode j2 = jn.getSeriesElement(1);
		JobNode j3 = jn.getSeriesElement(2);
		assertEquals("jobA[0>4]", j1.stringify(true));
		assertEquals("jobB[9>13]", j2.stringify(true));
		assertEquals("jobC[17>21]", j3.stringify(true));
	}

	@Test
	public void simpleParallelSequence() {
		js = parse("<jobA & jobB>");
		assertEquals("<jobA & jobB>", js.getJobDefinitionText());
		assertEquals("<jobA & jobB>", js.stringify());
		JobNode jn = js.getJobNode();
		assertTrue(jn.isSplit());
		assertEquals(2, jn.getSeriesLength());
		JobNode j1 = jn.getSeriesElement(0);
		JobNode j2 = jn.getSeriesElement(1);
		assertEquals("jobA[1>5]", j1.stringify(true));
		assertEquals("jobB[8>12]", j2.stringify(true));
	}

	@Test
	public void tripleParallelSequence() {
		js = parse("<jobA & jobB & jobC>");
		assertEquals("<jobA & jobB & jobC>", js.getJobDefinitionText());
		assertEquals("<jobA & jobB & jobC>", js.stringify());
		JobNode jn = js.getJobNode();
		assertTrue(jn.isSplit());
		assertEquals(3, jn.getSeriesLength());
		JobNode js1 = jn.getSeriesElement(0);
		JobNode js2 = jn.getSeriesElement(1);
		JobNode js3 = jn.getSeriesElement(2);
		assertEquals("jobA[1>5]", js1.stringify(true));
		assertEquals("jobB[8>12]", js2.stringify(true));
		assertEquals("jobC[15>19]", js3.stringify(true));
		assertTrue(js1 instanceof JobReference);
		assertEquals("jobA", ((JobReference) js1).getName());
		assertEquals("jobB", ((JobReference) js2).getName());
		assertEquals("jobC", ((JobReference) js3).getName());
	}

	@Test
	public void parentheses2() {
		js = parse("<(jobA || jobB || jobC) & jobC>");
		assertEquals("<(jobA || jobB || jobC) & jobC>", js.getJobDefinitionText());
		assertEquals("<jobA || jobB || jobC & jobC>", js.stringify());
	}

	@Test
	public void simpleParallelAndSequential() {
		js = parse("<jobA || jobA2 & jobB || jobB2 & jobC || jobC2>");
		assertEquals("<jobA || jobA2 & jobB || jobB2 & jobC || jobC2>", js.getJobDefinitionText());
		assertEquals("<jobA || jobA2 & jobB || jobB2 & jobC || jobC2>", js.stringify());
		JobNode jobNode = js.getJobNode();
		assertTrue(jobNode instanceof Split);
		Split pjs = (Split) jobNode;
		assertEquals(3, pjs.getSeriesLength());
		JobNode js1 = pjs.getSeriesElement(0);
		JobNode js2 = pjs.getSeriesElement(1);
		JobNode js3 = pjs.getSeriesElement(2);
		assertEquals("jobA[1>5] || jobA2[9>14]", js1.stringify(true));
		assertEquals("jobB[17>21] || jobB2[25>30]", js2.stringify(true));
		assertEquals("jobC[33>37] || jobC2[41>46]", js3.stringify(true));
		assertEquals(2, js1.getSeriesLength());
		assertEquals(2, js2.getSeriesLength());
		assertEquals(2, js3.getSeriesLength());
	}

	@Test
	public void funnyJobNames() {
		js = parse("a.b.c");
		assertEquals("a.b.c", ((JobReference) js.getJobNode()).getName());
		js = parse("a_b.c");
		assertEquals("a_b.c", ((JobReference) js.getJobNode()).getName());
		js = parse("a_b_c");
		assertEquals("a_b_c", ((JobReference) js.getJobNode()).getName());
	}

	@Test
	public void nestedSplit1() {
		js = parse("<<jobA & jobB> & jobC>");
		assertEquals("<<jobA & jobB> & jobC>", js.getJobDefinitionText());
		assertEquals("<<jobA & jobB> & jobC>", js.stringify());
		JobNode jn = js.getJobNode();
		assertTrue(jn.isSplit());
		Split pjs = (Split) jn;
		assertEquals(2, pjs.getSeriesLength());
		JobNode j1 = pjs.getSeriesElement(0);
		assertTrue(j1.isSplit());
		assertEquals(2, j1.getSeriesLength());
		JobNode j2 = pjs.getSeriesElement(1);
		assertFalse(j2.isSplit());
	}

	@Test
	public void nestedSplit2() {
		js = parse("<jobA & <jobB & jobC> & jobD>");
		assertEquals("<jobA & <jobB & jobC> & jobD>", js.getJobDefinitionText());
		assertEquals("<jobA & <jobB & jobC> & jobD>", js.stringify());
		JobNode jn = js.getJobNode();

		assertTrue(jn.isSplit());
		assertEquals(3, jn.getSeriesLength());

		JobNode j1 = jn.getSeriesElement(0);
		assertTrue(j1.isJobDescriptor());
		assertEquals("jobA", j1.stringify());

		JobNode j2 = jn.getSeriesElement(1);
		assertTrue(j2.isSplit());
		assertEquals(2, j2.getSeriesLength());

		JobNode j3 = jn.getSeriesElement(2);
		assertTrue(j3.isJobDescriptor());
		assertEquals("jobD", j3.stringify());
	}

	@Test
	public void mixSplitSerial() {
		js = parse("<jobA & jobB || jobC & jobD>");
		assertEquals("<jobA & jobB || jobC & jobD>", js.getJobDefinitionText());
		assertEquals("<jobA & jobB || jobC & jobD>", js.stringify());
		JobNode jn = js.getJobNode();
		assertTrue(jn.isSplit());
		assertEquals(3, jn.getSeriesLength());
		JobNode ja = jn.getSeriesElement(0);
		assertTrue(ja.isJobDescriptor());
		assertTrue(((JobDescriptor) ja).isReference());
		assertEquals("jobA", ((JobReference) ja).getName());
		JobNode jb = jn.getSeriesElement(1);
		assertTrue(jb.isFlow());
		assertEquals("jobB", jb.getSeriesElement(0).stringify());
		assertEquals("jobC", jb.getSeriesElement(1).stringify());
	}

	@Test
	public void splitJoin() {
		js = parse("<jobA & jobB> || jobC");
		assertEquals("<jobA & jobB> || jobC", js.getJobDefinitionText());
		assertEquals("<jobA & jobB> || jobC", js.stringify());
	}

	@Test
	public void inlineJobDefinition() {
		String dsl = "jobModuleA jobNameA";
		if (JobParser.SUPPORTS_INLINE_JOB_DEFINITIONS) {
			js = parse(dsl);
			JobNode jn = js.getJobNode();
			assertTrue(jn.isJobDescriptor());
			JobDefinition jd = (JobDefinition) jn;
			assertFalse(jd.isReference());
			assertTrue(jd.isDefinition());
			assertEquals("jobModuleA", jd.getJobModuleName());
			assertEquals("jobNameA", jd.getJobName());
		}
		else {
			checkForParseError(dsl, JobDSLMessage.EXPECTED_FLOW_OR_SPLIT_CHARS, 11, "jobNameA");
		}
	}

	@Test
	public void singleTransition() {
		js = parse("foo | completed = bar");
		JobNode jn = js.getJobNode();
		assertTrue(jn.isJobDescriptor());
		JobDescriptor jd = (JobDescriptor) jn;
		List<Transition> transitions = jd.getTransitions();
		assertEquals(1, transitions.size());
		assertEquals("completed", transitions.get(0).getStateName());
		assertEquals("bar", transitions.get(0).getTargetJobName());
	}

	@Test
	public void doubleTransition() {
		js = parse("foo | completed = bar | wibble=wobble");
		JobNode jn = js.getJobNode();
		assertTrue(jn.isJobDescriptor());
		JobDescriptor jd = (JobDescriptor) jn;
		List<Transition> transitions = jd.getTransitions();
		assertEquals(2, transitions.size());
		assertEquals("completed", transitions.get(0).getStateName());
		assertEquals("bar", transitions.get(0).getTargetJobName());
		assertEquals("wibble", transitions.get(1).getStateName());
		assertEquals("wobble", transitions.get(1).getTargetJobName());
	}

	@Test
	public void inlineJobDefWithTransition() {
		if (JobParser.SUPPORTS_INLINE_JOB_DEFINITIONS) {
			js = parse("foo bar --aaa=bbb | completed = bar | wibble=wobble");
			JobNode jn = js.getJobNode();
			assertTrue(jn.isJobDescriptor());
			JobDescriptor jd = (JobDescriptor) jn;
			assertTrue(jd.isDefinition());
			JobDefinition jobdef = (JobDefinition) jd;
			assertEquals("foo", jobdef.getJobModuleName());
			assertEquals("bar", jobdef.getJobName());
			ArgumentNode[] args = jobdef.getArguments();
			assertEquals(1, args.length);
			assertEquals("aaa", args[0].getName());
			assertEquals("bbb", args[0].getValue());
			List<Transition> transitions = jd.getTransitions();
			assertEquals(2, transitions.size());
			assertEquals("completed", transitions.get(0).getStateName());
			assertEquals("bar", transitions.get(0).getTargetJobName());
			assertEquals("wibble", transitions.get(1).getStateName());
			assertEquals("wobble", transitions.get(1).getTargetJobName());
		}
		else {
			checkForParseError("foo bar --aaa=bbb | completed = bar | wibble=wobble",
					JobDSLMessage.EXPECTED_FLOW_OR_SPLIT_CHARS, 4, "bar");
		}
	}

	@Test
	public void wildcardTransition() {
		js = parse("foo | '*' = wibble");
		assertEquals("foo | '*' = wibble", js.stringify());
		js = parse("foo | \"*\" = wibble");
		assertEquals("foo | \"*\" = wibble", js.stringify());
	}

	@Test
	public void splitWithTransition() {
		js = parse("<foo | completed=kill & bar>");
		assertEquals("<foo | completed=kill & bar>", js.getJobDefinitionText());
		assertEquals("<foo | completed = kill & bar>", js.stringify());
	}

	@Test
	public void splitWithTransitionLiterals() {
		js = parse("<foo | 'completed'=kill & bar>");
		assertEquals("<foo | 'completed'=kill & bar>", js.getJobDefinitionText());
		assertEquals("<foo | 'completed' = kill & bar>", js.stringify());
	}

	@Test
	public void multiLine() {
		js = parse("<foo\n"
				+ "  | 'completed'=kill\n"
				+ "  | '*' = custard\n"
				+ "  & bar>");
		assertEquals("<foo\n"
				+ "  | 'completed'=kill\n"
				+ "  | '*' = custard\n"
				+ "  & bar>", js.getJobDefinitionText());
		assertEquals("<foo | 'completed' = kill | '*' = custard & bar>", js.stringify());
		//		assertEquals("<foo | 'completed' = kill | '*' = custard & bar>", js.format());
	}

	@Test
	public void toGraphBlank() {
		js = parse("");
		Graph g = js.toGraph();
		assertEquals(toExpectedGraph("n:0:START,n:1:END,l:0:1"), g.toJSON());
		js = parse(" ");
		g = js.toGraph();
		assertEquals(toExpectedGraph("n:0:START,n:1:END,l:0:1"), g.toJSON());
	}

	@Test
	public void toGraphSequence() {
		js = parse("foo || bar");
		assertEquals(toExpectedGraph("n:0:START,n:1:foo,n:2:bar,n:3:END,l:0:1,l:1:2,l:2:3"),
				js.toGraph().toJSON());
	}


	@Test
	public void complexToGraphAndBack() {
		js = parse(
				"aaa | 'FAILED' = iii || <bbb | 'FAILED' = iii | '*' = $END & ccc | 'FAILED'=jjj | '*'=$END> ||ddd| 'FAILED'=iii||eee| 'FAILED'=iii||fff| 'FAILED'=iii||<ggg| 'FAILED'=kkk | '*'=$END& hhh| 'FAILED'=kkk| '*'=$END>    ");
		checkDSLToGraphAndBackToDSL(
				"aaa | 'FAILED' = iii || <bbb | 'FAILED' = iii | '*' = $END & ccc | 'FAILED' = jjj | '*' = $END> || ddd | 'FAILED' = iii || eee | 'FAILED' = iii || fff | 'FAILED' = iii || <ggg | 'FAILED' = kkk | '*' = $END & hhh | 'FAILED' = kkk | '*' = $END>");
	}

	@Test
	public void toGraph$END() {
		js = parse("foo | oranges=$END");

		// AST creation (and DSL printing from it)
		assertEquals("foo | oranges = $END", js.stringify());

		// Graph creation
		// Note: node '2' does not exist, it was an $END node for the transition, when we discovered that was
		// going to be joined to the final $END node we re-routed the links targeting '2' to the real end
		// node and removed it
		assertEquals(toExpectedGraph("n:0:START,n:1:foo,n:3:END,l:0:1,l:1:3:transitionName=oranges"),
				js.toGraph().toJSON());

		// Graph to DSL
		checkDSLToGraphAndBackToDSL("foo | oranges = $END");

		// toXML
		assertEquals(loadXml("end1"), js.toXML("test1", true));
	}

	@Test
	public void toGraph$END2() {
		js = parse("aaa | foo = $END | B = bbb | '*' = ccc || bbb || ccc");

		// AST creation (and DSL printing from it)
		assertEquals("aaa | foo = $END | B = bbb | '*' = ccc || bbb || ccc", js.stringify());

		// {"nodes":[{"id":"0","name":"START"},{"id":"1","name":"aaa"},{"id":"3","name":"bbb"},{"id":"4","name":"ccc"},
		//           {"id":"5","name":"END"}],
		//  "links":[{"from":"0","to":"1"},{"from":"1","to":"5","properties":{"transitionName":"foo"}},
		//           {"from":"1","to":"3","properties":{"transitionName":"B"}},
		//           {"from":"1","to":"4","properties":{"transitionName":"'*'"}},
		//           {"from":"3","to":"4"},{"from":"4","to":"5"}]}
		// Graph creation
		assertEquals(
				toExpectedGraph("n:0:START,n:1:aaa,n:3:bbb,n:4:ccc,n:5:END," +
						"l:0:1,l:1:5:transitionName=foo,l:1:3:transitionName=B,l:1:4:transitionName='*',l:3:4,l:4:5"),
				js.toGraph().toJSON());

		// Graph to DSL
		checkDSLToGraphAndBackToDSL("aaa | foo = $END | B = bbb | '*' = ccc || bbb || ccc");

		// toXML
		assertEquals(loadXml("end2"), js.toXML("test1", true));
	}

	@Test
	public void toGraph$END3() {
		// The trailing 'bbb' is redundant here...
		js = parse("aaa | foo = $END | B = bbb | '*' = $END || bbb");

		// AST creation (and DSL printing from it)
		assertEquals("aaa | foo = $END | B = bbb | '*' = $END || bbb", js.stringify());

		// Graph creation
		//		assertEquals(
		//				toExpectedGraph("n:0:START,n:1:aaa,n:2:bbb,n:3:ccc,n:4:END," +
		//						"l:0:1,l:2:3,l:1:4:transitionName=foo,l:1:2:transitionName=B,l:1:3:transitionName='*',l:3:4"),
		//				js.toGraph().toJSON());

		// Graph to DSL
		checkDSLToGraphAndBackToDSL("aaa | foo = $END | B = bbb | '*' = $END");

		// toXML
		assertEquals(loadXml("end3"), js.toXML("test1", true));
	}


	@Test
	public void toGraph$FAIL() {
		js = parse("foo | oranges=$FAIL");

		// AST creation (and DSL printing from it)
		assertEquals("foo | oranges = $FAIL", js.stringify());

		// {"nodes":[{"id":"0","name":"START"},{"id":"1","name":"foo"},{"id":"2","name":"FAIL"},
		//           {"id":"3","name":"END"}],
		//  "links":[{"from":"0","to":"1"},{"from":"1","to":"2","properties":{"transitionName":"oranges"}},
		//           {"from":"2","to":"3"}]}
		// Graph creation
		assertEquals(toExpectedGraph("n:0:START,n:1:foo,n:2:FAIL,n:3:END,l:0:1,l:1:2:transitionName=oranges,l:2:3"),
				js.toGraph().toJSON());

		// Graph to DSL
		checkDSLToGraphAndBackToDSL("foo | oranges = $FAIL");

		// XML creation
		assertEquals(loadXml("fail1"), js.toXML("test1", true));
	}

	@Test
	public void toGraphInlineJob() {
		if (JobParser.SUPPORTS_INLINE_JOB_DEFINITIONS) {
			js = parse("filejdbc foo --aaa=bbb || bar");
			assertEquals(toExpectedGraph(
					"n:0:START,n:1:foo:meta-jobModuleName=filejdbc;aaa=bbb,n:2:bar,n:3:END,l:0:1,l:1:2,l:2:3"),
					js.toGraph().toJSON());
		}
		else {
			checkForParseError("filejdbc foo --aaa=bbb || bar", JobDSLMessage.EXPECTED_FLOW_OR_SPLIT_CHARS, 9, "foo");
		}
	}

	@Test
	public void toGraphSequence2() {
		js = parse("foo || bar || boo");
		assertEquals(toExpectedGraph("n:0:START,n:1:foo,n:2:bar,n:3:boo,n:4:END,l:0:1,l:1:2,l:2:3,l:3:4"),
				js.toGraph().toJSON());
	}

	@Test
	public void toGraphSplit() {
		js = parse("<foo & bar> || boo");
		assertEquals(
				toExpectedGraph("n:0:START,n:1:foo,n:2:bar,n:3:boo,n:4:END,l:0:1,l:0:2,l:1:3,l:2:3,l:3:4"),
				js.toGraph().toJSON());
	}

	// TODO should & end the boo job name? Don't think it does right now
	// 	js = parse("<foo | completed=boo& bar> || boo");

	@Test
	public void toGraphWithTransition() throws Exception {
		js = parse("<foo | completed=goo & bar> || boo || goo");
		// {"nodes":[{"id":"0","name":"START"},{"id":"1","name":"foo"},{"id":"2","name":"goo"},
		//           {"id":"3","name":"bar"},{"id":"4","name":"boo"},{"id":"5","name":"goo"},{"id":"6","name":"END"}],
		//  "links":[{"from":"0","to":"1"},{"from":"1","to":"2","properties":{"transitionName":"completed"}},
		//           {"from":"0","to":"3"},{"from":"2","to":"4"},{"from":"3","to":"4"},{"from":"4","to":"5"},
		//           {"from":"5","to":"6"}]}
		assertEquals(
				toExpectedGraph(
						"n:0:START,n:1:foo,n:2:goo,n:3:bar,n:4:boo,n:5:goo,n:6:END,l:0:1,l:1:2:transitionName=completed,l:0:3,l:2:4,l:3:4,l:4:5,l:5:6"),
				js.toGraph().toJSON());
	}

	@Test
	public void toGraphWithTransition2() {
		// The target transition node is not elsewhere on the list
		js = parse("<foo | completed=hoo & bar> || boo || goo");
		checkDSLToGraphAndBackToDSL("<foo | completed = hoo & bar> || boo || goo");
		// {"nodes":[{"id":"0","name":"START"},{"id":"1","name":"foo"},{"id":"2","name":"hoo"},
		//           {"id":"3","name":"bar"},{"id":"4","name":"boo"},{"id":"5","name":"goo"},{"id":"6","name":"END"}],
		//  "links":[{"from":"0","to":"1"},{"from":"1","to":"2","properties":{"transitionName":"completed"}},
		//           {"from":"0","to":"3"},{"from":"2","to":"4"},{"from":"3","to":"4"},{"from":"4","to":"5"},
		//           {"from":"5","to":"6"}]}
		assertEquals(
				toExpectedGraph(
						"n:0:START,n:1:foo,n:2:hoo,n:3:bar,n:4:boo,n:5:goo,n:6:END,l:0:1,l:1:2:transitionName=completed,l:0:3,l:2:4,l:3:4,l:4:5,l:5:6"),
				js.toGraph().toJSON());
	}

	@Test
	public void toXML() {
		js = parse("foo");
		assertEquals(loadXml("simpleJob"), js.toXML("test1", true));
	}

	@Test
	public void toXMLallExitsCovered() {
		js = parse("aaa | foo = $END | B = bbb | '*' = ccc || bbb || ccc");
		assertEquals(loadXml("allExitsCovered"), js.toXML("test1", true));
	}

	@Ignore
	@Test
	public void toXMLSqoopExample() {
		js = parse("<(sqoop-6e44 | 'FAILED' = kill1\n" +
				"  || sqoop-e07a | 'FAILED' = kill1) & \n" +
				" (sqoop-035f | 'FAILED' = kill2\n" +
				"  || sqoop-9408 | 'FAILED' = kill2\n" +
				"  || sqoop-a6e0 | 'FAILED' = kill2\n" +
				"  || sqoop-e522 | 'FAILED' = kill2\n" +
				"  || shell-b521 | 'FAILED' = kill2) & \n" +
				" (sqoop-6420 | 'FAILED' = kill3)>");
		assertEquals(loadXml("sqoopJob"), js.toXML("test1", true));
	}

	@Test
	public void toXMLFlow() {
		js = parse("foo || bar");
		assertEquals(loadXml("simpleFlow"), js.toXML("test1", true));
	}

	@Test
	public void toXMLFlowDup() {
		js = parse("foo || foo");
		assertEquals(loadXml("xmlFlowDup"), js.toXML("test1", true));
	}

	@Test
	public void toXMLSplitDup() {
		js = parse("<foo & foo>");
		assertEquals(loadXml("xmlSplitDup"), js.toXML("test1", true));
	}

	@Test
	public void toXMLFlowWithTransition() {
		js = parse("foo || bar | failed = goo");
		assertEquals(loadXml("simpleFlowWithTransition"), js.toXML("test1", true));
	}

	@Test
	public void toXMLSplitWithTransition() {
		js = parse("<foo | failed = boo & bar | failed = goo>");
		assertEquals(loadXml("simpleSplitWithTransition"), js.toXML("test1", true));
	}

	// Both failed transitions point to 'boo' - as it is across a split there should be separate boo
	// entries (with appropriate IDs)
	@Test
	public void toXMLSplitWithTransition2() {
		js = parse("<foo | failed = boo & bar | failed = boo>");
		assertEquals(loadXml("simpleSplitWithTransition2"), js.toXML("test1", true));
	}

	@Test
	public void toXMLWithForwardReference() {
		js = parse("aaa | failed = ccc || bbb || ccc || ddd");
		assertEquals(loadXml("forwardReference"), js.toXML("test1", true));
	}

	@Test
	public void toXMLSplitWithTransition3() {
		js = parse("<foo | failed = boo | error = boo & bar | failed = boo>");
		assertEquals(loadXml("simpleSplitWithTransition3"), js.toXML("test1", true));
	}

	@Test
	public void toXMLSplitWithTransition4() {
		js = parse("<foo | failed = boo | error = boo || goo & bar | failed = boo>");
		assertEquals(loadXml("simpleSplitWithTransition4"), js.toXML("test1", true));
	}

	@Test
	public void toXMLFlowWithMultiTransitionToSameTarget() {
		js = parse("foo | failed = bbb || bar | failed = bbb");
		assertEquals(loadXml("simpleFlowWithTransition2"), js.toXML("test1", true));
	}

	@Test
	public void toXMLFlowWithMultiTransitionOnSameJobToSameTarget() {
		js = parse("foo | failed = bbb | error = bbb || bar | failed = bbb");
		assertEquals("foo | failed = bbb | error = bbb || bar | failed = bbb", js.stringify());
		assertEquals(loadXml("simpleFlowWithTransition3"), js.toXML("test1", true));
	}

	@Test
	public void toXMLFlowWithLotsTransitions() {
		js = parse("foo | failed = bbb | error = boo || bar | failed = bbb");
		assertEquals(loadXml("simpleFlowWithTransition4"), js.toXML("test1", true));
	}

	@Test
	public void toXMLFlowDupModule() {
		js = parse("foo || foo");
		assertEquals(loadXml("simpleFlowDupModule"), js.toXML("test1", true));
	}

	@Test
	public void toXMLFlowSplitDupModule() {
		js = parse("foo || <foo & foo>");
		assertEquals(loadXml("flowSplitDupModule"), js.toXML("test1", true));
	}

	@Test
	public void toXML1() {
		js = parse("aaa || <bbb & ccc> || farg || ddd");
		assertXml("xml1", js.toXML("foo_COMPOSED", true));
	}

	@Test
	public void lotsOfTransitions() {
		js = parse("aaa | 'FAILED'=iii ||< bbb | 'FAILED'=iii & ccc | 'FAILED'=jjj> ||" +
				"ddd| 'FAILED'=iii||eee| 'FAILED'=iii||fff| 'FAILED'=iii||<ggg| 'FAILED'=kkk & hhh| 'FAILED'=kkk>");
		assertEquals(
				"aaa | 'FAILED' = iii || <bbb | 'FAILED' = iii & ccc | 'FAILED' = jjj> || "
						+
						"ddd | 'FAILED' = iii || eee | 'FAILED' = iii || fff | 'FAILED' = iii || <ggg | 'FAILED' = kkk & hhh | 'FAILED' = kkk>",
				js.stringify());
		assertXml("lotsOfTransitions", js.toXML("lotsOfTransitions", true));
	}

	@Test
	public void lotsOfTransitions2() {
		js = parse("aaa | 'FAILED'=iii ||< bbb | 'FAILED'=iii|| zzz  & ccc | 'FAILED'=jjj> ||" +
				"ddd| 'FAILED'=iii||eee| 'FAILED'=iii||fff| 'FAILED'=iii||<ggg| 'FAILED'=kkk & hhh| 'FAILED'=kkk>");
		assertEquals(
				"aaa | 'FAILED' = iii || <bbb | 'FAILED' = iii || zzz & ccc | 'FAILED' = jjj> || "
						+
						"ddd | 'FAILED' = iii || eee | 'FAILED' = iii || fff | 'FAILED' = iii || <ggg | 'FAILED' = kkk & hhh | 'FAILED' = kkk>",
				js.stringify());
		assertXml("lotsOfTransitions2", js.toXML("lotsOfTransitions2", true));
	}

	@Test
	public void toXML2() {
		js = parse("aaa|| bbb || farg | 'FAILED'=ccc ||ddd");
		assertXml("xml2", js.toXML("foo_COMPOSED", true));
	}

	@Test
	public void toXMLFlowDupModule2() {
		js = parse("foo || bar || foo || <goo & foo> || foo");
		assertEquals(loadXml("simpleFlowDupModule2"), js.toXML("test1", true));
	}

	@Test
	public void toXMLFlowRefOptions() {
		js = parse("foo || bar || boo --timeout=100");
		assertEquals(loadXml("simpleFlowOptions"), js.toXML("test1", true));
	}

	@Test
	public void toXMLFlowRefOptions2() {
		js = parse("foo || bar || boo --timeout=100 --pollInterval=999 ");
		assertEquals(loadXml("simpleFlowOptions2"), js.toXML("test1", true));
	}

	@Test
	public void toXMLFlowRefOptions3() {
		js = parse("foo || bar || boo --zz='abc' --timeout=100 --pollInterval=999 --xx=99 --yy=custard ");
		assertEquals(loadXml("simpleFlowOptions3"), js.toXML("test1", true));
		js = parse("foo || bar || boo --zz='abc' --timeout=100 --pollInterval=999 --xx=99 --yy=custard");
		assertEquals(loadXml("simpleFlowOptions3"), js.toXML("test1", true));
	}

	@Test
	public void toXMLSplit() {
		js = parse("<foo & bar>");
		assertEquals(loadXml("simpleSplit"), js.toXML("test1", true));
	}

	@Test
	public void toXmlFlowSplit() {
		js = parse("AA || <BB & CC> || DD");
		assertEquals(loadXml("flowSplit"), js.toXML("test1", true));
	}

	@Test
	public void toXmlLongFlowSplit() {
		js = parse("AA || <BB || CC & DD> || EE");
		assertEquals(loadXml("flowSplit2"), js.toXML("test1", true));
	}

	@Test
	public void inlineJobDefinitionWithOptions() {
		if (JobParser.SUPPORTS_INLINE_JOB_DEFINITIONS) {
			js = parse("jobModuleA jobNameA --foo=bar --boo=gar");
			JobNode jn = js.getJobNode();
			assertTrue(jn.isJobDescriptor());
			JobDefinition jd = (JobDefinition) jn;
			assertFalse(jd.isReference());
			assertTrue(jd.isDefinition());
			assertEquals("jobModuleA", jd.getJobModuleName());
			assertEquals("jobNameA", jd.getJobName());
			ArgumentNode[] args = jd.getArguments();
			assertEquals(2, args.length);
			assertEquals("--foo=bar", args[0].stringify());
			assertEquals("--boo=gar", args[1].stringify());
		}
		else {
			checkForParseError(
					"jobModuleA jobNameA --foo=bar --boo=gar",
					JobDSLMessage.EXPECTED_FLOW_OR_SPLIT_CHARS, 11, "jobNameA");
		}
	}

	@Test
	public void extraneousDataError() {
		String jobSpecification = "<a & b> rubbish";
		checkForParseError(jobSpecification, JobDSLMessage.UNEXPECTED_DATA_AFTER_JOBSPEC, 8, "rubbish");
	}

	@Test
	public void incorrectTransition() {
		checkForParseError("foo | || = bar", JobDSLMessage.EXPECTED_TRANSITION_NAME, 6, "||");
	}

	@Test
	public void spacesInTransitionNameRequireQuotes() {
		checkForParseError("foo | abc def = bar", JobDSLMessage.EXPECTED_EQUALS_AFTER_TRANSITION_NAME, 10, "def");
		parse("foo | 'abc def' = bar");
	}

	@Test
	public void inlineJobDefWithTwoArguments() {
		String dsl = "foo foo --name=value --x=y";
		if (JobParser.SUPPORTS_INLINE_JOB_DEFINITIONS) {
			js = parse(dsl);
			List<JobDefinition> jds = js.getJobDefinitions();
			assertEquals(1, jds.size());
			assertEquals(dsl, jds.get(0).stringify(false));
		}
		else {
			checkForParseError(dsl, JobDSLMessage.EXPECTED_FLOW_OR_SPLIT_CHARS, 4, "foo");
		}
	}

	@Test
	public void inlineJobDefDottedArgName() {
		if (JobParser.SUPPORTS_INLINE_JOB_DEFINITIONS) {
			js = parse("foo foo --nam.eee=value --x=y");
			List<JobDefinition> jds = js.getJobDefinitions();
			assertEquals(1, jds.size());
			assertEquals("foo foo --nam.eee=value --x=y", jds.get(0).stringify(false));
		}
		else {
			checkForParseError("foo foo --nam.eee=value --x=y",
					JobDSLMessage.EXPECTED_FLOW_OR_SPLIT_CHARS, 4, "foo");
			checkForParseError("foo foo --nam.eee=value --x=y",
					JobDSLMessage.EXPECTED_FLOW_OR_SPLIT_CHARS, 4, "foo");
		}
	}

	@Test
	public void inlineJobDefDottedArgValue() {
		if (JobParser.SUPPORTS_INLINE_JOB_DEFINITIONS) {
			js = parse("foo foo --name=abc.def --x=y");
			List<JobDefinition> jds = js.getJobDefinitions();
			assertEquals(1, jds.size());
			assertEquals("foo foo --name=abc.def --x=y", jds.get(0).stringify(false));
		}
		else {
			checkForParseError("foo foo --name=abc.def --x=y",
					JobDSLMessage.EXPECTED_FLOW_OR_SPLIT_CHARS, 4, "foo");
		}
	}

	@Test
	public void inlineJobDefWithArgumentsAndTransitions() {
		if (JobParser.SUPPORTS_INLINE_JOB_DEFINITIONS) {
			js = parse("foo foo --name=value --x=y | aaa=bbb | ccc=ddd | eee=fff");
			List<JobDefinition> jds = js.getJobDefinitions();
			assertEquals(1, jds.size());
			assertEquals("foo foo --name=value --x=y | aaa = bbb | ccc = ddd | eee = fff", jds.get(0).stringify(false));

			js = parse("foo foo --name=value --x=y | aaa=bbb | ccc=ddd | eee=fff || bar bart || goo good");
			jds = js.getJobDefinitions();
			assertEquals(3, jds.size());
			assertEquals("bar bart", jds.get(1).stringify(false));
		}
		else {
			checkForParseError("foo foo --name=value --x=y | aaa=bbb | ccc=ddd | eee=fff",
					JobDSLMessage.EXPECTED_FLOW_OR_SPLIT_CHARS, 4, "foo");
			checkForParseError("foo foo --name=value --x=y | aaa = bbb | ccc = ddd | eee = fff",
					JobDSLMessage.EXPECTED_FLOW_OR_SPLIT_CHARS, 4, "foo");
			checkForParseError("foo foo --name=value --x=y | aaa=bbb | ccc=ddd | eee=fff || bar bart || goo good",
					JobDSLMessage.EXPECTED_FLOW_OR_SPLIT_CHARS, 4, "foo");
		}
	}

	@Test
	public void inlineJobDefinitionNeedsName() {
		checkForParseError("foo || transform --expression=new StringBuilder(payload).reverse() || bar",
				JobDSLMessage.UNEXPECTED_DATA_AFTER_JOBSPEC, 34);
	}

	@Test
	public void argumentsNeedQuotesAroundArgValueWithSpaces() {
		String dsl = "foo || transform bar --expression=new StringBuilder(payload).reverse() || bar";
		if (JobParser.SUPPORTS_INLINE_JOB_DEFINITIONS) {
			checkForParseError(dsl, JobDSLMessage.UNEXPECTED_DATA_AFTER_JOBSPEC, 38);
			// TODO [asc] How/when are the quotes removed for these argument values?
			js = parse("foo || transform bar --expression='new StringBuilder(payload).reverse()' || bar");
		}
		else {
			checkForParseError(dsl, JobDSLMessage.EXPECTED_FLOW_OR_SPLIT_CHARS, 17, "bar");
		}
	}

	@Test
	public void moduleArguments_xd1613() {
		if (JobParser.SUPPORTS_INLINE_JOB_DEFINITIONS) {
			// notice no space between the ' and final >
			js = parse(
					"transform X --expression='payload.toUpperCase()' || filter Y --expression='payload.length() > 4'");
			assertEquals("payload.toUpperCase()", js.getJobDefinition("X").getArguments()[0].getValue());
			assertEquals("payload.length() > 4", js.getJobDefinition("Y").getArguments()[0].getValue());

			js = parse(
					"time || transform X --expression='T(org.joda.time.format.DateTimeFormat).forPattern(\"yyyy-MM-dd HH:mm:ss\").parseDateTime(payload)'");
			assertEquals(
					"T(org.joda.time.format.DateTimeFormat).forPattern(\"yyyy-MM-dd HH:mm:ss\").parseDateTime(payload)",
					js.getJobDefinition("X").getArguments()[0].getValue());

			// allow for pipe/semicolon if quoted
			js = parse("http || transform X --outputType='text/plain|charset=UTF-8' || log");
			assertEquals("text/plain|charset=UTF-8", js.getJobDefinition("X").getArguments()[0].getValue());

			js = parse("http || transform X --outputType='text/plain;charset=UTF-8' || log");
			assertEquals("text/plain;charset=UTF-8", js.getJobDefinition("X").getArguments()[0].getValue());

			// Want to treat all of 'hi'+payload as the argument value
			js = parse("http || transform X --expression='hi'+payload || log");
			assertEquals("'hi'+payload", js.getJobDefinition("X").getArguments()[0].getValue());

			// Want to treat all of payload+'hi' as the argument value
			js = parse("http || transform X --expression=payload+'hi' || log");
			assertEquals("payload+'hi'", js.getJobDefinition("X").getArguments()[0].getValue());

			// Alternatively, can quote all around it to achieve the same thing
			js = parse("http || transform X --expression='payload+''hi''' || log");
			assertEquals("payload+'hi'", js.getJobDefinition("X").getArguments()[0].getValue());
			js = parse("http || transform X --expression='''hi''+payload' || log");
			assertEquals("'hi'+payload", js.getJobDefinition("X").getArguments()[0].getValue());


			js = parse("http || transform X --expression=\"payload+'hi'\" || log");
			assertEquals("payload+'hi'", js.getJobDefinition("X").getArguments()[0].getValue());
			js = parse("http || transform X --expression=\"'hi'+payload\" || log");
			assertEquals("'hi'+payload", js.getJobDefinition("X").getArguments()[0].getValue());

			js = parse("http || transform X --expression=payload+'hi'--param2='foobar' || log");
			assertEquals("payload+'hi'--param2='foobar'", js.getJobDefinition("X").getArguments()[0].getValue());

			js = parse("http || transform X --expression='hi'+payload--param2='foobar' || log");
			assertEquals("'hi'+payload--param2='foobar'", js.getJobDefinition("X").getArguments()[0].getValue());

			// This also works, which is cool
			js = parse("http || transform X --expression='hi'+'world' || log");
			assertEquals("'hi'+'world'", js.getJobDefinition("X").getArguments()[0].getValue());
			js = parse("http || transform X --expression=\"'hi'+'world'\" || log");
			assertEquals("'hi'+'world'", js.getJobDefinition("X").getArguments()[0].getValue());

			js = parse("http || filter X --expression=payload.matches('hello world') || log");
			assertEquals("payload.matches('hello world')", js.getJobDefinition("X").getArguments()[0].getValue());

			js = parse("http || transform X --expression='''hi''' || log");
			assertEquals("'hi'", js.getJobDefinition("X").getArguments()[0].getValue());

			js = parse("http || transform X --expression=\"''''hi''''\" || log");
			assertEquals("''''hi''''", js.getJobDefinition("X").getArguments()[0].getValue());

			js = parse("http asdf --port=9014 || filter X --expression=\"payload == 'foo'\" || log");
			assertEquals("payload == 'foo'", js.getJobDefinition("X").getArguments()[0].getValue());

			js = parse("http asdf --port=9014 || filter X --expression='new Foo()' || log");
			assertEquals("new Foo()", js.getJobDefinition("X").getArguments()[0].getValue());

			js = parse("http || transform XX --expression='payload.replace(\"abc\", \"\")' || log");
			assertEquals("payload.replace(\"abc\", \"\")", js.getJobDefinition("XX").getArguments()[0].getValue());

			js = parse("http || transform XX --expression='payload.replace(\"abc\", '''')' || log");
			assertEquals("payload.replace(\"abc\", '')", js.getJobDefinition("XX").getArguments()[0].getValue());
		}
		else {
			checkForParseError(
					"transform X --expression='payload.toUpperCase()' || filter Y --expression='payload.length() > 4'",
					JobDSLMessage.EXPECTED_FLOW_OR_SPLIT_CHARS, 10, "X");
		}
	}

	// Parameters must be constructed via adjacent tokens
	@Test
	public void needAdjacentTokensForParameters() {
		if (JobParser.SUPPORTS_INLINE_JOB_DEFINITIONS) {
			checkForParseError("foo a -- name=value", JobDSLMessage.NO_WHITESPACE_BEFORE_ARG_NAME, 9);
			checkForParseError("foo a --name =value", JobDSLMessage.NO_WHITESPACE_BEFORE_ARG_EQUALS, 13);
			checkForParseError("foo a --name= value", JobDSLMessage.NO_WHITESPACE_BEFORE_ARG_VALUE, 14);
		}
		else {
			checkForParseError("foo a -- name=value", JobDSLMessage.EXPECTED_FLOW_OR_SPLIT_CHARS, 4, "a");
		}
	}

	// --

	/**
	 * Create a parser for test use.
	 */
	private JobParser getParser() {
		return new JobParser();
	}

	private JobSpecification parse(String jobSpecification) {
		JobSpecification jobSpec = getParser().parse(jobSpecification);
		return jobSpec;
	}

	/**
	 * Convert the supplied internal DSL (for test usage only) into a graph against which we can
	 * compare the actual output from the graph creation code.
	 * @param graphTestDSL the shorthand DSL for a graph description
	 * @return a JSON string representing the complete graph
	 */
	private String toExpectedGraph(String graphTestDSL) {
		StringBuilder s = new StringBuilder();
		s.append("{");
		s.append("\"nodes\":[");
		StringTokenizer st = new StringTokenizer(graphTestDSL, ",");
		boolean onLinksNow = false;
		int counter = 0;
		while (st.hasMoreElements()) {
			String element = st.nextToken();
			StringTokenizer elementTokenizer = new StringTokenizer(element, ":");
			String type = elementTokenizer.nextToken();
			if (type.equals("n")) {
				String nodeId = elementTokenizer.nextToken();
				String nodeName = elementTokenizer.nextToken();
				if (counter > 0) {
					s.append(",");
				}
				s.append("{\"id\":\"" + nodeId + "\",\"name\":\"" + nodeName + "\"");
				if (elementTokenizer.hasMoreTokens()) {
					String propertiesAndMetadata = elementTokenizer.nextToken();

					StringBuilder metadata = new StringBuilder();
					StringBuilder properties = new StringBuilder();

					StringTokenizer propertyOrMetadataTokenizer = new StringTokenizer(propertiesAndMetadata, ";");
					metadata.append(",\"metadata\":{");
					properties.append(",\"properties\":{");
					int propertyCount = 0;
					int metadataCount = 0;
					while (propertyOrMetadataTokenizer.hasMoreTokens()) {
						String propertyOrMetadata = propertyOrMetadataTokenizer.nextToken();
						int equals = propertyOrMetadata.indexOf("=");
						String key = propertyOrMetadata.substring(0, equals);
						String value = propertyOrMetadata.substring(equals + 1);
						if (key.startsWith("meta-")) {
							key = key.substring(5);
							if (metadataCount > 0) {
								metadata.append(",");
							}
							metadata.append("\"" + key + "\":\"" + value + "\"");
							metadataCount++;
						}
						else {
							if (propertyCount > 0) {
								s.append(",");
							}
							properties.append("\"" + key + "\":\"" + value + "\"");
							propertyCount++;
						}
					}
					if (metadataCount > 0) {
						s.append(metadata.toString()).append("}");
					}
					if (propertyCount > 0) {
						s.append(properties.toString());
					}
					s.append("}");
				}
				s.append("}");
			}
			else {
				if (!onLinksNow) {
					s.append("],\"links\":[");
					onLinksNow = true;
					counter = 0;
				}
				if (counter > 0) {
					s.append(",");
				}
				String sourceId = elementTokenizer.nextToken();
				String targetId = elementTokenizer.nextToken();
				s.append("{\"from\":\"" + sourceId + "\",\"to\":\"" + targetId + "\"");
				if (elementTokenizer.hasMoreTokens()) {
					String properties = elementTokenizer.nextToken();
					StringTokenizer propertyTokenizer = new StringTokenizer(properties, ";");
					s.append(",\"properties\":{");
					int propertyCount = 0;
					while (propertyTokenizer.hasMoreTokens()) {
						if (propertyCount > 0) {
							s.append(",");
						}
						String property = propertyTokenizer.nextToken();
						int equals = property.indexOf("=");
						String key = property.substring(0, equals);
						String value = property.substring(equals + 1);
						s.append("\"" + key + "\":\"" + value + "\"");
					}
					s.append("}");
				}
				s.append("}");
			}
			counter++;
		}
		s.append("]}");
		return s.toString();
	}

	/**
	 * Load a resource XML file that represents the expected test output.
	 */
	private String loadXml(String xmlfile) {
		try {
			String resourceName = this.getClass().getPackage().getName().toString().replace('.', File.separatorChar)
					+ File.separator
					+ xmlfile + ".xml";
			InputStream istream = getClass().getClassLoader().getResourceAsStream(resourceName);
			BufferedInputStream bis = new BufferedInputStream(istream);
			byte[] theData = new byte[10000000];
			int dataReadSoFar = 0;
			byte[] buffer = new byte[1024];
			int read = 0;
			while ((read = bis.read(buffer)) != -1) {
				System.arraycopy(buffer, 0, theData, dataReadSoFar, read);
				dataReadSoFar += read;
			}
			bis.close();
			byte[] returnData = new byte[dataReadSoFar];
			System.arraycopy(theData, 0, returnData, 0, dataReadSoFar);
			return new String(returnData);
		}
		catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}

	private void checkForParseError(String jobSpecification, JobDSLMessage msg, int pos, Object... inserts) {
		try {
			JobSpecification js = parse(jobSpecification);
			fail("expected to fail but parsed " + js.stringify());
		}
		catch (JobSpecificationException e) {
			e.printStackTrace();
			assertEquals(msg, e.getMessageCode());
			assertEquals(pos, e.getPosition());
			if (inserts != null) {
				for (int i = 0; i < inserts.length; i++) {
					assertEquals(inserts[i], e.getInserts()[i]);
				}
			}
		}
	}

	private void checkDSLToGraphAndBackToDSL(String specification) {
		js = parse(specification);
		Graph g = js.toGraph();
		assertEquals(specification, g.toDSLText());
	}

	private void assertToken(TokenKind kind, String string, int start, int end, Token t) {
		assertEquals(kind, t.kind);
		assertEquals(string, t.getKind().hasPayload() ? t.stringValue() : new String(t.getKind().getTokenChars()));
		assertEquals(start, t.startpos);
		assertEquals(end, t.endpos);
	}

	private void assertXml(String xmlFileName, String expectedText) {
		assertEquals(loadXml(xmlFileName), expectedText);
	}
}
