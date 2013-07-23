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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Test;
import org.springframework.xd.dirt.stream.EnhancedStreamParser;

/**
 * Parse streams and verify either the correct abstract syntax tree is 
 * produced or the current exception comes out.
 * 
 * @author Andy Clement
 */
public class StreamConfigParserTests {
	
	// Simplest form is a single module name - although it is not a well formed stream
	@Test
	public void oneModule() {
		StreamsNode ast = parse("foo");
		assertEquals("Streams[foo][(ModuleNode:foo:0>3)]",ast.stringify(true));
	}

	// Naming a stream is done via <name>=<something> where <something> might be 0 or more modules/channels
	@Test
	public void streamNaming() {
		StreamsNode ast = new StreamConfigParser().parse("mystream = foo");
		assertEquals("Streams[mystream = foo][mystream = (ModuleNode:foo:11>14)]",ast.stringify(true));
	}
	
	// Pipes are used to connect modules
	@Test
	public void twoModules() {
		StreamsNode ast = new StreamConfigParser().parse("foo | bar");
		assertEquals("Streams[foo | bar][(ModuleNode:foo:0>3)(ModuleNode:bar:6>9)]",ast.stringify(true));
	}
	
	// Modules can be labeled
	@Test
	public void moduleLabels() {
		StreamsNode ast = new StreamConfigParser().parse("label: http");
		assertEquals("Streams[label: http][((Label:label:0>5) ModuleNode:http:0>11)]",ast.stringify(true));
	}
	
	@Test
	public void moduleLabels2() {
		StreamsNode ast = new StreamConfigParser().parse("label: label2: http | label3: foo");
		assertEquals("Streams[label: label2: http | label3: foo][((Label:label:0>5) (Label:label2:7>13) ModuleNode:http:0>19)((Label:label3:22>28) ModuleNode:foo:22>33)]",ast.stringify(true));
	}
	
	@Test
	public void moduleLabels3() {
		StreamsNode ast = new StreamConfigParser().parse("food = http | label3: foo");
		assertEquals("Streams[food = http | label3: foo][food = (ModuleNode:http:7>11)((Label:label3:14>20) ModuleNode:foo:14>25)]",ast.stringify(true));
	}
	
	// Modules can take parameters
	@Test
	public void oneModuleWithParam() {
		StreamsNode ast = new StreamConfigParser().parse("foo --name=value");
		assertEquals("Streams[foo --name=value][(ModuleNode:foo --name=value:0>16)]",ast.stringify(true));
	}

	
	// Modules can take two parameters
	@Test
	public void oneModuleWithTwoParams() {
		StreamsNode ast = new StreamConfigParser().parse("foo --name=value --x=y");
		assertTrue(ast instanceof StreamsNode);
		StreamsNode sn = ast;
		List<ModuleNode> moduleNodes = sn.getModuleNodes();
		assertEquals(1,moduleNodes.size());

		ModuleNode mn = moduleNodes.get(0);
		assertEquals("foo",mn.getName());
		ArgumentNode[] args = mn.getArguments();
		assertNotNull(args);
		assertEquals(2,args.length);
		assertEquals("name",args[0].getName());
		assertEquals("value",args[0].getValue());
		assertEquals("x",args[1].getName());
		assertEquals("y",args[1].getValue());

		assertEquals("Streams[foo --name=value --x=y][(ModuleNode:foo --name=value --x=y:0>22)]",ast.stringify(true));
	}

	@Test
	public void testHorribleTap() {
		String stream = "tap @twitter";
		StreamConfigParser parser = new StreamConfigParser();
		StreamsNode ast = parser.parse(stream);
		assertEquals("Streams[tap @twitter][(ModuleNode:tap --channel=twitter.0:0>12)]",ast.stringify(true));
	}

	// Stream definitions can be across lines
	@Test
	public void testMultiline() {
		String stream = "foo\nbar";
		StreamConfigParser parser = new StreamConfigParser();
		StreamsNode ast = parser.parse(stream);
		List<StreamNode> streams = ast.getStreams();
		assertEquals(2,streams.size());
	}

	@Test
	public void testMultiline2() {
		String stream = "foo  ;  bar";
		StreamConfigParser parser = new StreamConfigParser();
		StreamsNode ast = parser.parse(stream);
		List<StreamNode> streams = ast.getStreams();
		assertEquals(2,streams.size());
	}

	@Test
	public void testParameters() {
		String module = "gemfire_cq --query='Select * from /Stocks where symbol=''VMW''' --regionName=foo --foo=bar";
		StreamConfigParser parser = new StreamConfigParser();
		StreamsNode ast = parser.parse(module);
		ModuleNode gemfireModule = ast.getModule("gemfire_cq");
		Properties parameters = gemfireModule.getArgumentsAsProperties();
		assertEquals(3, parameters.size());
		assertEquals("Select * from /Stocks where symbol='VMW'", parameters.get("query"));
		assertEquals("foo", parameters.get("regionName"));
		assertEquals("bar", parameters.get("foo"));

		module = "test";
		parameters = parser.parse(module).getModule("test").getArgumentsAsProperties();
		assertEquals(0, parameters.size());

		module = "foo --x=1 --y=two ";
		parameters = parser.parse(module).getModule("foo").getArgumentsAsProperties();
		assertEquals(2, parameters.size());
		assertEquals("1", parameters.get("x"));
		assertEquals("two", parameters.get("y"));

		module = "foo --x=1a2b --y=two ";
		parameters = parser.parse(module).getModule("foo").getArgumentsAsProperties();
		assertEquals(2, parameters.size());
		assertEquals("1a2b", parameters.get("x"));
		assertEquals("two", parameters.get("y"));

		module = "foo --x=2";
		parameters = parser.parse(module).getModule("foo").getArgumentsAsProperties();
		assertEquals(1, parameters.size());
		assertEquals("2", parameters.get("x"));

		module = "--foo = bar";
		try {
			parser.parse(module);
			fail(module + " is invalid. Should throw exception");
		} catch (Exception e) {
			// success
		}
	}

	@Test
	public void testInvalidModules() {
		String config = "test | foo--x=13";
		EnhancedStreamParser parser = new EnhancedStreamParser();
		try {
			parser.parse("t", config);
			fail(config + " is invalid. Should throw exception");
		} catch (Exception e) {
			// success
		}
	}
	
	@Test
	public void testDirtyTapSupport() {
//		StreamsNode ast = 
		new StreamConfigParser().parse("one", "foo | transform --expression=--payload | bar");
		StreamsNode ast2 = new StreamConfigParser().parse("two", "tap one.foo");
		assertEquals("Streams[tap one.foo][(ModuleNode:tap --channel=one.0:0>11)]",ast2.stringify(true));
		StreamsNode ast3 = new StreamConfigParser().parse("two", "tap one.transform");
		assertEquals("Streams[tap one.transform][(ModuleNode:tap --channel=one.1:0>17)]",ast3.stringify(true));
	}
	
	@Test
	public void tapWithModuleReference() {
		StreamsNode ast = parse("tap foo > file");
		// TODO verify source positions for source channel - include tap?
		assertEquals("Streams[tap foo > file][tap (foo:4>7)>(ModuleNode:file:10>14)]",ast.stringify(true));
	}
	
	@Test
	public void tapWithLabelReference() {
		parse("mystream = http | filter | group1: transform | group2: transform | file");
		StreamsNode ast = parse("tap mystream > file");
		assertEquals("Streams[tap mystream > file][tap (mystream[[channel:mystream.0]])>(ModuleNode:file)]",ast.stringify());
		ast = parse("tap filter > file");
		assertEquals("Streams[tap filter > file][tap (filter[[channel:mystream.1]])>(ModuleNode:file)]",ast.stringify());
		ast = parse("tap mystream.filter > file");
		assertEquals("Streams[tap mystream.filter > file][tap (mystream.filter[[channel:mystream.1]])>(ModuleNode:file)]",ast.stringify());
		ast = parse("tap group1 > file");
		assertEquals("Streams[tap group1 > file][tap (group1[[channel:mystream.2]])>(ModuleNode:file)]",ast.stringify());
		ast = parse("tap mystream.group2 > file");
		assertEquals("Streams[tap mystream.group2 > file][tap (mystream.group2[[channel:mystream.3]])>(ModuleNode:file)]",ast.stringify());
	}
	
	@Test
	public void tapWithQualifiedModuleReference() {
		StreamsNode ast = parse("tap mystream.foo > file");
		assertEquals("Streams[tap mystream.foo > file][tap (mystream.foo:4>16)>(ModuleNode:file:19>23)]",ast.stringify(true));

		parse("mystream = bar | foo | file");
		ast = parse("tap mystream.foo > file");
		assertEquals("Streams[tap mystream.foo > file][tap (mystream.foo[[channel:mystream.1]])>(ModuleNode:file)]",ast.stringify(false));
	}
	
	@Test
	public void tapWithChannel() {
		StreamsNode ast = parse("tap :foo > file");
		// TODO verify source positions for source channel - include tap?
		assertEquals("Streams[tap :foo > file][tap (:foo:4>8)>(ModuleNode:file:11>15)]",ast.stringify(true));
	}
	
	@Test
	public void tapWithQualifiedChannel() {
		StreamsNode ast = parse("tap :mystream.foo > file");
		assertEquals("Streams[tap :mystream.foo > file][tap (:mystream.foo:4>17)>(ModuleNode:file:20>24)]",ast.stringify(true));
	}

	@Test
	public void expressions_xd159() {
		StreamsNode ast = new StreamConfigParser().parse("foo | transform --expression=--payload | bar");
		ModuleNode mn = ast.getModule("transform");
		Properties props = mn.getArgumentsAsProperties();
		assertEquals("--payload",props.get("expression"));
	}

	@Test
	public void expressions_xd159_2() {
		// need quotes around an argument value with a space in it
		checkForParseError("foo | transform --expression=new StringBuilder(payload).reverse() | bar",XDDSLMessages.UNEXPECTED_DATA,46);
	}

	@Test
	public void expressions_xd159_3() {
		StreamsNode ast = new StreamConfigParser().parse("foo |  transform --expression='new StringBuilder(payload).reverse()' | bar");
		ModuleNode mn = ast.getModule("transform");
		Properties props = mn.getArgumentsAsProperties();
		assertEquals("new StringBuilder(payload).reverse()",props.get("expression"));
	}

	@Test
	public void expressions_xd159_4() {
		StreamsNode ast = new StreamConfigParser().parse("foo |  transform --expression=\"'Hello, world!'\" | bar");
		ModuleNode mn = ast.getModule("transform");
		Properties props = mn.getArgumentsAsProperties();
		assertEquals("'Hello, world!'",props.get("expression"));
		ast = new StreamConfigParser().parse("foo |  transform --expression='''Hello, world!''' | bar");
		mn = ast.getModule("transform");
		props = mn.getArgumentsAsProperties();
		assertEquals("'Hello, world!'",props.get("expression"));
		checkForParseError("foo |  transform --expression=''Hello, world!'' | bar",XDDSLMessages.UNEXPECTED_DATA,37);
	}

	@Test
	public void expressions_gh1() {
		StreamsNode ast = new StreamConfigParser().parse("http --port=9014 | filter --expression=\"payload == 'foo'\" | log");
		ModuleNode mn = ast.getModule("filter");
		Properties props = mn.getArgumentsAsProperties();
		assertEquals("payload == 'foo'",props.get("expression"));
	}

	@Test
	public void expressions_gh1_2() {
		StreamsNode ast = new StreamConfigParser().parse("http --port=9014 | filter --expression='new Foo()' | log");
		ModuleNode mn = ast.getModule("filter");
		Properties props = mn.getArgumentsAsProperties();
		assertEquals("new Foo()",props.get("expression"));
	}
	
	// Job steps joined with '&'
	@Test
	public void jobsteps() {
		StreamsNode ast =parse("step1 & step2");
		assertEquals("Streams[step1 & step2][(ModuleNode:step1:isJobStep:0>5)(ModuleNode:step2:isJobStep:8>13)]",ast.stringify(true));
	}
	
	// TODO Job steps with parameters
	
	// Mixing job steps and regular modules
	@Test
	public void mixingStepsAndRegularModules() {
		StreamsNode ast =parse("http | step1 & step2 | file");
		assertEquals("Streams[http | step1 & step2 | file][(ModuleNode:http:0>4)(ModuleNode:step1:isJobStep:7>12)(ModuleNode:step2:isJobStep:15>20)(ModuleNode:file:23>27)]",ast.stringify(true));
	}

	@Test
	public void sinkChannel() {
		StreamsNode ast = parse("http > :foo");
		assertEquals("Streams[http > :foo][(ModuleNode:http:0>4)>(:foo:7>11)]",ast.stringify(true));
	}
	
	@Test
	public void qualifiedSinkChannelError() {
		// Only the source channel can be explicitly qualified the sink channel stream qualifier is implied
		checkForParseError("http > :mystream.foo", XDDSLMessages.UNEXPECTED_DATA_AFTER_STREAMDEF, 16, ".");
//		StreamsNode ast = parse("http > :mystream.foo");
//		assertEquals("Streams[http > :mystream.foo][(ModuleNode:http:0>4)>(:mystream.foo:7>20)]",ast.stringify(true));
	}
	
	@Test
	public void sourceChannel() {
		StreamsNode ast = parse(":foo > file");
		assertEquals("Streams[:foo > file][(:foo:0>4)>(ModuleNode:file:7>11)]",ast.stringify(true));
	}
	
	@Test
	public void qualifiedSourceChannel() {
		StreamsNode ast = parse(":mystream.foo > file");
		assertEquals("Streams[:mystream.foo > file][(:mystream.foo:0>13)>(ModuleNode:file:16>20)]",ast.stringify(true));
	}
	
	@Test
	public void substreams() {
		StreamsNode ast = parse("myhttp = http --port=9000; myhttp | file");
		StreamNode stream2 = ast.getStreamNodes().get(1);
		assertEquals("[(ModuleNode:http --port=9000:9>25)(ModuleNode:file:36>40)]",stream2.stringify(true));
	}
	
	@Test
	public void substreamsWithSourceChannels() {
		checkForParseError("myhttp = :foo > foo: filter --name=payload; myhttp | file",XDDSLMessages.NO_SOURCE_IN_SUBSTREAM,-1,"myhttp = :foo > foo: filter --name=payload");
		checkForParseError("myhttp = :foo > filter; myhttp | file",XDDSLMessages.NO_SOURCE_IN_SUBSTREAM,-1,"myhttp = :foo > filter");
		checkForParseError("myhttp = :aaa.foo > filter; myhttp | file",XDDSLMessages.NO_SOURCE_IN_SUBSTREAM,-1,"myhttp = :aaa.foo > filter");
		checkForParseError("myhttp = tap :foo > filter; myhttp | file",XDDSLMessages.NO_SOURCE_IN_SUBSTREAM,-1,"myhttp = tap :foo > filter");
		checkForParseError("myhttp = tap aaa.foo > filter; myhttp | file",XDDSLMessages.NO_SOURCE_IN_SUBSTREAM,-1,"myhttp = tap aaa.foo > filter");
		checkForParseError("myhttp = :foo > filter --name=payload; myhttp | file",XDDSLMessages.NO_SOURCE_IN_SUBSTREAM,-1,"myhttp = :foo > filter --name=payload");
	}

	@Test
	public void substreamsWithSinkChannels() {
		checkForParseError("myhttp = filter > :foo; file | myhttp",XDDSLMessages.NO_SINK_IN_SUBSTREAM,-1,"myhttp = filter > :foo");
	}

	@Test
	public void substreamsMultipleModules() {
		StreamsNode ast = parse("foo = transform --expression='abc' | transform --expression='def'; http | foo | file");
		StreamNode stream2 = ast.getStreamNodes().get(1);
		// TODO after macro insertion the source locations for the inserted modules are kind of meaningless, reset them?
		assertEquals("[(ModuleNode:http:67>71)(ModuleNode:transform --expression=abc:6>34)(ModuleNode:transform --expression=def:37>65)(ModuleNode:file:80>84)]",stream2.stringify(true));
	}
	
	@Test
	public void substreamsAdditionalParams() {
		StreamsNode ast = parse("myhttp = http --port=9000; myhttp --setting2=value2 | file");
		StreamNode stream2 = ast.getStreamNodes().get(1);
		assertEquals("[(ModuleNode:http --port=9000 --setting2=value2)(ModuleNode:file)]",stream2.stringify());
	}
	
	@Test
	public void substreamsOverrideParams() {
		StreamsNode ast = parse("myhttp = http --port=9000; myhttp --port=9010| file");
		StreamNode stream2 = ast.getStreamNodes().get(1);
		assertEquals("[(ModuleNode:http --port=9010)(ModuleNode:file)]",stream2.stringify());
	}
	
	@Test
	public void parameterizedStreams() {
		StreamsNode ast = parse("nameReplacer = transform --expression=payload.replaceAll('${name}','x'); http | nameReplacer --name='Andy' | file");
		StreamNode stream2 = ast.getStreamNodes().get(1);
		assertEquals("[(ModuleNode:http)(ModuleNode:transform --expression=payload.replaceAll('Andy','x'))(ModuleNode:file)]",stream2.stringify());
	}

	@Test
	public void parameterizedStreamsMissingValue() {
		checkForParseError("nameReplacer = transform --expression=payload.replaceAll('${name}','x'); http | nameReplacer --name2='Andy' | file",
				XDDSLMessages.MISSING_VALUE_FOR_VARIABLE, -1,"name");
	}

	@Test
	public void parameterizedStreamsMissingCloseCurly() {
		checkForParseError("nameReplacer = transform --expression=payload.replaceAll('${name','x'); http | nameReplacer --name2='Andy' | file",
				XDDSLMessages.VARIABLE_NOT_TERMINATED, -1,"--expression=payload.replaceAll('${name','x')");
	}
	
	@Test
	public void parameterizedStreamsDefaultValues() {
		StreamsNode ast = parse("nameReplacer = transform --expression=payload.replaceAll('${name:foo}','x'); http | nameReplacer | file");
		StreamNode stream2 = ast.getStreamNodes().get(1);
		assertEquals("[(ModuleNode:http)(ModuleNode:transform --expression=payload.replaceAll('foo','x'))(ModuleNode:file)]",stream2.stringify());
	}
	
	@Test
	public void parameterizedStreamsMixingItUp() {
		StreamsNode ast = parse("nameReplacer = transform --expression=payload.replaceAll('${name:foo}','x'); http | nameReplacer --setting2=value2 | file");
		StreamNode stream2 = ast.getStreamNodes().get(1);
		assertEquals("[(ModuleNode:http)(ModuleNode:transform --expression=payload.replaceAll('foo','x') --setting2=value2)(ModuleNode:file)]",stream2.stringify());
	}
	
	@Test
	public void parameterizedStreamsMixingItUp2() {
		StreamsNode ast = parse("nameReplacer = transform --a1=${foo} --b1=${bar}; http | nameReplacer --foo=abc --bar=def | file");
		StreamNode stream2 = ast.getStreamNodes().get(1);
		assertEquals("[(ModuleNode:http)(ModuleNode:transform --a1=abc --b1=def)(ModuleNode:file)]",stream2.stringify());
	}

	@Test
	public void parameterizedSubstreamsMultipleModules() {
		StreamsNode ast = parse("nameReplacer = transform --a1=${foo} | transform --b1=${bar}; http | nameReplacer --foo=abc --bar=def | file");
		StreamNode stream2 = ast.getStreamNodes().get(1);
		assertEquals("[(ModuleNode:http)(ModuleNode:transform --a1=abc)(ModuleNode:transform --b1=def)(ModuleNode:file)]",stream2.stringify());
	}
	
	@Test
	public void parameterizedSubstreamsMultipleModulesDefaultValues() {
		StreamsNode ast = parse("nameReplacer = transform --a1=${foo:default} | transform --b1=${bar}; http | nameReplacer --bar=def | file");
		StreamNode stream2 = ast.getStreamNodes().get(1);
		assertEquals("[(ModuleNode:http)(ModuleNode:transform --a1=default)(ModuleNode:transform --b1=def)(ModuleNode:file)]",stream2.stringify());
	}
	
	// TODO namespaces? StreamsNode ast =parse("mystreams.foo.bar = label.doo: step1 & label.bar: step2");
	
	// TODO Topology parsing?
//	@Test
//	public void topologyAssignment() {
//		parse("mystream = http | filter | group1: transform | group2: transform | file");
//		parse("group1.colocation=true");
//		parse("mystream.group1.colocation=true");
//		parse("mystream.filter.foobar=banana");
//		parse("group1.topology = [colocation:true]");
//	}
	
	@Test
	public void errorCases01() {
		checkForParseError(".",XDDSLMessages.EXPECTED_MODULENAME,0,".");
		checkForParseError(";",XDDSLMessages.EXPECTED_MODULENAME,0,";");
	}
	
	@Test
	public void errorCases02() {
		checkForParseError("foo--bar=yyy",XDDSLMessages.EXPECTED_WHITESPACE_AFTER_MODULE_BEFORE_ARGUMENT,3);
	}

	@Test
	public void errorCases03() {
		checkForParseError("foo-bar=yyy",XDDSLMessages.MISSING_CHARACTER,3,"-");
	}

	@Test
	public void errorCases04() {
		checkForParseError("foo bar=yyy",XDDSLMessages.UNEXPECTED_DATA_AFTER_STREAMDEF,4,"bar");
		checkForParseError("foo bar",XDDSLMessages.UNEXPECTED_DATA_AFTER_STREAMDEF,4,"bar");
	}
	
	@Test
	public void errorCases05() {
		checkForParseError("foo --",XDDSLMessages.OOD,6);
		checkForParseError("foo --bar",XDDSLMessages.OOD,9);
		checkForParseError("foo --bar=",XDDSLMessages.OOD,10);
	}
	
	@Test
	public void errorCases06() {
		checkForParseError("|",XDDSLMessages.EXPECTED_MODULENAME,0);
	}
	
	@Test
	public void errorCases07() {
		checkForParseError("foo > bar",XDDSLMessages.EXPECTED_CHANNEL_QUALIFIER,6,"bar");
		checkForParseError("foo > :",XDDSLMessages.OOD,7);
		checkForParseError("foo > :--2323",XDDSLMessages.EXPECTED_CHANNEL_NAME,7,"--");
		checkForParseError("foo > :*",XDDSLMessages.UNEXPECTED_DATA,7,"*");
	}
	
	@Test
	public void errorCases08() {
		checkForParseError(":foo | bar",XDDSLMessages.EXPECTED_MODULENAME,0,":");
	}
	
	@Test
	public void errorCases09() {
		checkForParseError(":foo > :boo",XDDSLMessages.EXPECTED_MODULENAME,7,":");
	}
	
	@Test
	public void errorCases10() {
		checkForParseError("* = http | file",XDDSLMessages.UNEXPECTED_DATA,0,"*");
		checkForParseError(": = http | file",XDDSLMessages.ILLEGAL_STREAM_NAME,0,":");
	}

	// Parameters must be constructed via adjacent tokens
	@Test
	public void needAdjacentTokens() {
		checkForParseError("foo -- name=value",XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_NAME,7);
		checkForParseError("foo --name =value",XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_EQUALS,11);
		checkForParseError("foo --name= value",XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_VALUE,12);
	}

	// ---
	
	@After
	public void reset() {
		StreamConfigParser.reset();
	}
	
	StreamsNode parse(String streamDefinition) {
		return new StreamConfigParser().parse(streamDefinition);
	}
	

	private void checkForParseError(String stream,XDDSLMessages msg,int pos,String... inserts) {
		try {
			StreamsNode sn = new StreamConfigParser().parse(stream);
			fail("expected to fail but parsed "+sn.stringify());
		} catch (DSLException e) {
			e.printStackTrace();
			assertEquals(msg,e.getMessageCode());
			assertEquals(pos,e.getPosition());
			if (inserts!=null) {
				for (int i=0;i<inserts.length;i++) {
					assertEquals(inserts[i], e.getInserts()[i]);
				}
			}
		}
	}
}