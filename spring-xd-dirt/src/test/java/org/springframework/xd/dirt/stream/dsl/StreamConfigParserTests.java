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

import org.junit.Test;

import org.springframework.xd.dirt.stream.EnhancedStreamParser;
import org.springframework.xd.dirt.stream.dsl.ast.ArgumentNode;
import org.springframework.xd.dirt.stream.dsl.ast.ModuleNode;
import org.springframework.xd.dirt.stream.dsl.ast.StreamNode;
import org.springframework.xd.dirt.stream.dsl.ast.StreamsNode;

/**
 * @author Andy Clement
 */
public class StreamConfigParserTests {

	@Test
	public void oneModule() {
		StreamsNode ast = new StreamConfigParser().parse("foo");
		assertEquals("Streams[foo][(ModuleNode:foo:0>3)]",ast.stringify());
	}

	@Test
	public void moduleAlias() {
		StreamsNode ast = new StreamConfigParser().parse("mystream = foo");
		assertEquals("Streams[mystream = foo][mystream = (ModuleNode:foo:11>14)]",ast.stringify());
	}

	@Test
	public void twoModules() {
		StreamsNode ast = new StreamConfigParser().parse("foo | bar");
		assertEquals("Streams[foo | bar][(ModuleNode:foo:0>3)(ModuleNode:bar:6>9)]",ast.stringify());
	}

	@Test
	public void oneModuleWithParam() {
		StreamsNode ast = new StreamConfigParser().parse("foo --name=value");
		assertEquals("Streams[foo --name=value][(ModuleNode:foo --name=value:0>16)]",ast.stringify());
	}

	@Test
	public void needAdjacentTokens() {
		checkForParseError("foo -- name=value",XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_NAME,7);
		checkForParseError("foo --name =value",XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_EQUALS,11);
		checkForParseError("foo --name= value",XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_VALUE,12);
	}

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

		assertEquals("Streams[foo --name=value --x=y][(ModuleNode:foo --name=value --x=y:0>22)]",ast.stringify());
	}

	@Test
	public void testHorribleTap() {
		String stream = "tap @twitter";
		StreamConfigParser parser = new StreamConfigParser();
		StreamsNode ast = parser.parse(stream);
		assertEquals("Streams[tap @twitter][(ModuleNode:tap --channel=twitter.0:0>12)]",ast.stringify());
	}

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
		String module = "gemfire-cq --query='Select * from /Stocks where symbol=''VMW''' --regionName=foo --foo=bar";
		StreamConfigParser parser = new StreamConfigParser();
		StreamsNode ast = parser.parse(module);
		ModuleNode gemfireModule = ast.getModule("gemfire-cq");
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
		assertEquals("Streams[tap one.foo][(ModuleNode:tap --channel=one.0:0>11)]",ast2.stringify());
		StreamsNode ast3 = new StreamConfigParser().parse("two", "tap one.transform");
		assertEquals("Streams[tap one.transform][(ModuleNode:tap --channel=one.1:0>17)]",ast3.stringify());
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

	// ---

	private void checkForParseError(String stream,XDDSLMessages msg,int pos) {
		try {
			new StreamConfigParser().parse(stream);
			fail("expected to fail");
		} catch (DSLParseException e) {
//			e.printStackTrace();
			assertEquals(msg,e.getMessageCode());
			assertEquals(pos,e.getPosition());
		}
	}
}