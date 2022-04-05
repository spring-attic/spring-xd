/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.stream.dsl;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.springframework.xd.dirt.stream.ParsingContext.stream;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.data.repository.CrudRepository;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.XDStreamParser;
import org.springframework.xd.dirt.zookeeper.EmbeddedZooKeeper;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.module.options.DefaultModuleOptionsMetadataResolver;

/**
 * Parse streams and verify either the correct abstract syntax tree is produced or the current exception comes out.
 *
 * @author Andy Clement
 * @author David Turanski
 */
public class StreamConfigParserTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private StreamNode sn;

	private EmbeddedZooKeeper zk = new EmbeddedZooKeeper();

	private ZooKeeperConnection zooKeeperConnection;

	private TestRepository testRepository = new TestRepository();

	@Before
	public void setUp() {
		zk = new EmbeddedZooKeeper();
		zk.start();
		zooKeeperConnection = new ZooKeeperConnection("localhost:" + zk.getClientPort());
	}

	// This is not a well formed stream but we are testing single module parsing
	@Test
	public void oneModule() {
		sn = parse("foo");
		assertEquals(1, sn.getModuleNodes().size());
		ModuleNode mn = sn.getModule("foo");
		assertEquals("foo", mn.getName());
		assertEquals(0, mn.getArguments().length);
		assertEquals(0, mn.startpos);
		assertEquals(3, mn.endpos);
	}

	@Test
	public void hyphenatedModuleName() {
		sn = parse("gemfire-cq");
		assertEquals("[(ModuleNode:gemfire-cq:0>10)]", sn.stringify(true));
	}

	// Just to make the testing easier the parser supports stream naming easier.
	@Test
	public void streamNaming() {
		sn = parse("mystream = foo");
		assertEquals("[mystream = (ModuleNode:foo:11>14)]", sn.stringify(true));
		assertEquals("mystream", sn.getName());
	}

	// Test if the DSLException thrown when the stream name is same as that of any of its modules' names.
	@Test
	public void testInvalidStreamName() {
		String streamName = "bar";
		String stream = "foo | bar";
		checkForParseError(streamName, stream, XDDSLMessages.STREAM_NAME_MATCHING_MODULE_NAME,
				stream.indexOf(streamName), streamName);
	}

	// Pipes are used to connect modules
	@Test
	public void twoModules() {
		StreamNode ast = parse("foo | bar");
		assertEquals("[(ModuleNode:foo:0>3)(ModuleNode:bar:6>9)]", ast.stringify(true));
	}

	// Modules can be labeled
	@Test
	public void moduleLabels() {
		StreamNode ast = parse("label: http");
		assertEquals("[((Label:label:0>5) ModuleNode:http:0>11)]", ast.stringify(true));
	}

	@Test
	public void moduleLabels3() {
		StreamNode ast = parse("food = http | label3: foo");
		assertEquals(
				"[food = (ModuleNode:http:7>11)((Label:label3:14>20) ModuleNode:foo:14>25)]",
				ast.stringify(true));

		sn = parse("http | foo:bar | file");
		assertEquals("[(ModuleNode:http)((Label:foo) ModuleNode:bar)(ModuleNode:file)]", sn.stringify());

		checkForParseError("http | foo: goggle: bar | file", XDDSLMessages.UNEXPECTED_DATA_AFTER_STREAMDEF,
				18);
		checkForParseError("http | foo :bar | file", XDDSLMessages.NO_WHITESPACE_BETWEEN_LABEL_NAME_AND_COLON, 11);
	}

	// Modules can take parameters
	@Test
	public void oneModuleWithParam() {
		StreamNode ast = parse("foo --name=value");
		assertEquals("[(ModuleNode:foo --name=value:0>16)]", ast.stringify(true));
	}

	// Modules can take two parameters
	@Test
	public void oneModuleWithTwoParams() {
		StreamNode sn = parse("foo --name=value --x=y");
		List<ModuleNode> moduleNodes = sn.getModuleNodes();
		assertEquals(1, moduleNodes.size());

		ModuleNode mn = moduleNodes.get(0);
		assertEquals("foo", mn.getName());
		ArgumentNode[] args = mn.getArguments();
		assertNotNull(args);
		assertEquals(2, args.length);
		assertEquals("name", args[0].getName());
		assertEquals("value", args[0].getValue());
		assertEquals("x", args[1].getName());
		assertEquals("y", args[1].getValue());

		assertEquals("[(ModuleNode:foo --name=value --x=y:0>22)]", sn.stringify(true));
	}

	@Test
	public void testParameters() {
		String module = "gemfire-cq --query='Select * from /Stocks where symbol=''VMW''' --regionName=foo --foo=bar";
		StreamNode ast = parse(module);
		ModuleNode gemfireModule = ast.getModule("gemfire-cq");
		Properties parameters = gemfireModule.getArgumentsAsProperties();
		assertEquals(3, parameters.size());
		assertEquals("Select * from /Stocks where symbol='VMW'", parameters.get("query"));
		assertEquals("foo", parameters.get("regionName"));
		assertEquals("bar", parameters.get("foo"));

		module = "test";
		parameters = parse(module).getModule("test").getArgumentsAsProperties();
		assertEquals(0, parameters.size());

		module = "foo --x=1 --y=two ";
		parameters = parse(module).getModule("foo").getArgumentsAsProperties();
		assertEquals(2, parameters.size());
		assertEquals("1", parameters.get("x"));
		assertEquals("two", parameters.get("y"));

		module = "foo --x=1a2b --y=two ";
		parameters = parse(module).getModule("foo").getArgumentsAsProperties();
		assertEquals(2, parameters.size());
		assertEquals("1a2b", parameters.get("x"));
		assertEquals("two", parameters.get("y"));

		module = "foo --x=2";
		parameters = parse(module).getModule("foo").getArgumentsAsProperties();
		assertEquals(1, parameters.size());
		assertEquals("2", parameters.get("x"));

		module = "--foo = bar";
		try {
			parse(module);
			fail(module + " is invalid. Should throw exception");
		}
		catch (Exception e) {
			// success
		}
	}

	@Test
	public void testInvalidModules() {
		String config = "test | foo--x=13";
		XDStreamParser parser = new XDStreamParser(testRepository, mock(ModuleRegistry.class),
				new DefaultModuleOptionsMetadataResolver());
		try {
			parser.parse("t", config, stream);
			fail(config + " is invalid. Should throw exception");
		}
		catch (Exception e) {
			// success
		}
	}

	@Test
	public void tapWithLabelReference() {
		parse("mystream = http | filter | group1: transform | group2: transform | file");
		StreamNode ast = parse("tap:stream:mystream.group1 > file");
		// post resolution 'group1' is transformed to transform
		assertEquals("[(tap:stream:mystream.transform.2)>(ModuleNode:file)]", ast.stringify());
		// TODO: Index should still be present in this case
		ast = parse("tap:stream:mystream > file");
		assertEquals("[(tap:stream:mystream.http.0)>(ModuleNode:file)]", ast.stringify());
	}

	@Test
	public void tapWithQualifiedModuleReference() {
		parse("mystream = http | foobar | file");
		StreamNode sn = parse("tap:stream:mystream.foobar > file");
		assertEquals("[(tap:stream:mystream.foobar.1:0>26)>(ModuleNode:file:29>33)]", sn.stringify(true));
	}

	@Test
	public void tapOnNonExistentStreamFails() throws Exception {
		thrown.expect(StreamDefinitionException.class);
		thrown.expect(hasProperty("position", is(equalTo("tap:stream:".length()))));

		parse("tap:stream:mystream.foobar > file");
	}

	@Test
	public void tapOnNonExistentStreamFails2() throws Exception {
		thrown.expect(StreamDefinitionException.class);
		thrown.expect(hasProperty("position", is(equalTo("tap:stream:".length()))));

		parse("tap:stream:mystream.foobar.1 > file");
	}

	@Test
	public void tapOnNonExistentStreamModuleFails() throws Exception {
		parse("mystream = http | file");

		thrown.expect(StreamDefinitionException.class);
		thrown.expect(hasProperty("position", is(equalTo("tap:stream:mystream.".length()))));
		parse("tap:stream:mystream.foobar > log");
	}

	@Test
	public void expressions_xd159() {
		StreamNode ast = parse("foo | transform --expression=--payload | bar");
		ModuleNode mn = ast.getModule("transform");
		Properties props = mn.getArgumentsAsProperties();
		assertEquals("--payload", props.get("expression"));
	}

	@Test
	public void expressions_xd159_2() {
		// need quotes around an argument value with a space in it
		checkForParseError("foo | transform --expression=new StringBuilder(payload).reverse() | bar",
				XDDSLMessages.UNEXPECTED_DATA, 46);
	}

	@Test
	public void ensureStreamNamesValid_xd1344() {
		// Similar rules to a java identifier but also allowed '-' after the first char
		checkForIllegalStreamName("foo.bar", "http | transform | sink");
		checkForIllegalStreamName("-bar", "http | transform | sink");
		checkForIllegalStreamName(".bar", "http | transform | sink");
		checkForIllegalStreamName("foo-.-bar", "http | transform | sink");
		checkForIllegalStreamName("0foobar", "http | transform | sink");
		checkForIllegalStreamName("foo%bar", "http | transform | sink");
		parse("foo-bar", "http | transform | sink");
		parse("foo_bar", "http | transform | sink");
	}

	@Test
	public void expressions_xd159_3() {
		StreamNode ast = parse("foo |  transform --expression='new StringBuilder(payload).reverse()' | bar");
		ModuleNode mn = ast.getModule("transform");
		Properties props = mn.getArgumentsAsProperties();
		assertEquals("new StringBuilder(payload).reverse()", props.get("expression"));
	}

	@Test
	public void moduleArguments_xd1613() {
		StreamNode ast = null;

		// notice no space between the ' and final >
		ast = parse("queue:producer > transform --expression='payload.toUpperCase()' | filter --expression='payload.length() > 4'> queue:consumer");
		assertEquals("payload.toUpperCase()", ast.getModule("transform").getArguments()[0].getValue());
		assertEquals("payload.length() > 4", ast.getModule("filter").getArguments()[0].getValue());

		ast = parse("time | transform --expression='T(org.joda.time.format.DateTimeFormat).forPattern(\"yyyy-MM-dd HH:mm:ss\").parseDateTime(payload)'");
		assertEquals(
				"T(org.joda.time.format.DateTimeFormat).forPattern(\"yyyy-MM-dd HH:mm:ss\").parseDateTime(payload)",
				ast.getModule("transform").getArguments()[0].getValue());

		// allow for pipe/semicolon if quoted
		ast = parse("http | transform --outputType='text/plain|charset=UTF-8'  | log");
		assertEquals("text/plain|charset=UTF-8", ast.getModule("transform").getArguments()[0].getValue());

		ast = parse("http | transform --outputType='text/plain;charset=UTF-8'  | log");
		assertEquals("text/plain;charset=UTF-8", ast.getModule("transform").getArguments()[0].getValue());

		// Want to treat all of 'hi'+payload as the argument value
		ast = parse("http | transform --expression='hi'+payload | log");
		assertEquals("'hi'+payload", ast.getModule("transform").getArguments()[0].getValue());

		// Want to treat all of payload+'hi' as the argument value
		ast = parse("http | transform --expression=payload+'hi' | log");
		assertEquals("payload+'hi'", ast.getModule("transform").getArguments()[0].getValue());

		// Alternatively, can quote all around it to achieve the same thing
		ast = parse("http | transform --expression='payload+''hi''' | log");
		assertEquals("payload+'hi'", ast.getModule("transform").getArguments()[0].getValue());
		ast = parse("http | transform --expression='''hi''+payload' | log");
		assertEquals("'hi'+payload", ast.getModule("transform").getArguments()[0].getValue());

		ast = parse("http | transform --expression=\"payload+'hi'\" | log");
		assertEquals("payload+'hi'", ast.getModule("transform").getArguments()[0].getValue());
		ast = parse("http | transform --expression=\"'hi'+payload\" | log");
		assertEquals("'hi'+payload", ast.getModule("transform").getArguments()[0].getValue());

		ast = parse("http | transform --expression=payload+'hi'--param2='foobar' | log");
		assertEquals("payload+'hi'--param2='foobar'", ast.getModule("transform").getArguments()[0].getValue());

		ast = parse("http | transform --expression='hi'+payload--param2='foobar' | log");
		assertEquals("'hi'+payload--param2='foobar'", ast.getModule("transform").getArguments()[0].getValue());

		// This also works, which is cool
		ast = parse("http | transform --expression='hi'+'world' | log");
		assertEquals("'hi'+'world'", ast.getModule("transform").getArguments()[0].getValue());
		ast = parse("http | transform --expression=\"'hi'+'world'\" | log");
		assertEquals("'hi'+'world'", ast.getModule("transform").getArguments()[0].getValue());

		ast = parse("http | filter --expression=payload.matches('hello world') | log");
		assertEquals("payload.matches('hello world')", ast.getModule("filter").getArguments()[0].getValue());

		ast = parse("http | transform --expression='''hi''' | log");
		assertEquals("'hi'", ast.getModule("transform").getArguments()[0].getValue());

		ast = parse("http | transform --expression=\"''''hi''''\" | log");
		assertEquals("''''hi''''", ast.getModule("transform").getArguments()[0].getValue());
	}

	@Test
	public void expressions_xd159_4() {
		StreamNode ast = parse("foo |  transform --expression=\"'Hello, world!'\" | bar");
		ModuleNode mn = ast.getModule("transform");
		Properties props = mn.getArgumentsAsProperties();
		assertEquals("'Hello, world!'", props.get("expression"));
		ast = parse("foo |  transform --expression='''Hello, world!''' | bar");
		mn = ast.getModule("transform");
		props = mn.getArgumentsAsProperties();
		assertEquals("'Hello, world!'", props.get("expression"));
		// Prior to the change for XD-1613, this error should point to the comma:
		// checkForParseError("foo |  transform --expression=''Hello, world!'' | bar", XDDSLMessages.UNEXPECTED_DATA,
		// 37);
		// but now it points to the !
		checkForParseError("foo |  transform --expression=''Hello, world!'' | bar", XDDSLMessages.UNEXPECTED_DATA, 44);
	}

	@Test
	public void expressions_gh1() {
		StreamNode ast = parse("http --port=9014 | filter --expression=\"payload == 'foo'\" | log");
		ModuleNode mn = ast.getModule("filter");
		Properties props = mn.getArgumentsAsProperties();
		assertEquals("payload == 'foo'", props.get("expression"));
	}

	@Test
	public void expressions_gh1_2() {
		StreamNode ast = parse("http --port=9014 | filter --expression='new Foo()' | log");
		ModuleNode mn = ast.getModule("filter");
		Properties props = mn.getArgumentsAsProperties();
		assertEquals("new Foo()", props.get("expression"));
	}

	@Test
	public void sourceChannel() {
		StreamNode sn = parse("queue:foobar > file");
		assertEquals("[(queue:foobar:0>12)>(ModuleNode:file:15>19)]", sn.stringify(true));
	}

	@Test
	public void sinkChannel() {
		StreamNode sn = parse("http > queue:foo");
		assertEquals("[(ModuleNode:http:0>4)>(queue:foo:7>16)]", sn.stringify(true));
	}

	@Test
	public void channelVariants() {
		// Job is not a legal channel prefix
		checkForParseError("trigger > job:foo", XDDSLMessages.EXPECTED_CHANNEL_PREFIX_QUEUE_TOPIC, 10, "job");

		// This looks like a label and so file is treated as a sink!
		checkForParseError("queue: bar > file", XDDSLMessages.NO_WHITESPACE_IN_CHANNEL_DEFINITION, 7);

		// 'queue' looks like a module all by itself so everything after is unexpected
		checkForParseError("queue : bar > file", XDDSLMessages.NO_WHITESPACE_IN_CHANNEL_DEFINITION, 6);

		// 'queue' looks like a module all by itself so everything after is unexpected
		checkForParseError("queue :bar > file", XDDSLMessages.NO_WHITESPACE_IN_CHANNEL_DEFINITION, 6);

		checkForParseError("tap:queue: boo > file", XDDSLMessages.NO_WHITESPACE_IN_CHANNEL_DEFINITION, 11);
		checkForParseError("tap:queue :boo > file", XDDSLMessages.NO_WHITESPACE_IN_CHANNEL_DEFINITION, 10);
		checkForParseError("tap:queue : boo > file", XDDSLMessages.NO_WHITESPACE_IN_CHANNEL_DEFINITION, 10);

		checkForParseError("tap:stream:boo .xx > file", XDDSLMessages.NO_WHITESPACE_IN_CHANNEL_DEFINITION, 15);
		checkForParseError("tap:stream:boo . xx > file", XDDSLMessages.NO_WHITESPACE_IN_CHANNEL_DEFINITION, 15);
		checkForParseError("tap:stream:boo. xx > file", XDDSLMessages.NO_WHITESPACE_IN_CHANNEL_DEFINITION, 16);
		checkForParseError("tap:stream:boo.xx. yy > file", XDDSLMessages.NO_WHITESPACE_IN_CHANNEL_DEFINITION, 19);
		checkForParseError("tap:stream:boo.xx .yy > file", XDDSLMessages.NO_WHITESPACE_IN_CHANNEL_DEFINITION, 18);
		checkForParseError("tap:stream:boo.xx . yy > file", XDDSLMessages.NO_WHITESPACE_IN_CHANNEL_DEFINITION, 18);

		checkForParseError("tap:queue:boo.xx.yy > file", XDDSLMessages.ONLY_A_TAP_ON_A_STREAM_OR_JOB_CAN_BE_INDEXED, 13);

		sn = parse("wibble: http > queue:bar");
		assertEquals("[((Label:wibble) ModuleNode:http)>(queue:bar)]", sn.stringify());
	}

	@Test
	public void qualifiedSinkChannelError() {
		// Only the source channel supports a dotted suffix
		checkForParseError("http > queue:wibble.foo", XDDSLMessages.CHANNEL_INDEXING_NOT_ALLOWED, 19);
	}

	@Test
	public void sourceChannel2() {
		parse("foo = http | bar | file");
		StreamNode ast = parse("tap:stream:foo.bar > file");
		assertEquals("[(tap:stream:foo.bar.1:0>18)>(ModuleNode:file:21>25)]", ast.stringify(true));
		assertEquals("tap:stream:foo.bar.1", ast.getSourceChannelNode().getChannelName());
	}

	@Test
	public void sourceTapChannel() {
		StreamNode ast = parse("tap:queue:xxy > file");
		assertEquals("[(tap:queue:xxy:0>13)>(ModuleNode:file:16>20)]", ast.stringify(true));
	}

	@Test
	public void sourceTapChannel2() {
		parse("mystream = http | file");
		StreamNode ast = parse("tap:stream:mystream.http > file");
		assertEquals(
				"[(tap:stream:mystream.http.0:0>24)>(ModuleNode:file:27>31)]",
				ast.stringify(true));
	}

	@Test
	public void sourceTapChannelNoColon() {
		parse("mystream = http | file");
		StreamNode ast = null;
		SourceChannelNode sourceChannelNode = null;

		ast = parse("tap:stream:mystream.http > file");
		sourceChannelNode = ast.getSourceChannelNode();
		assertEquals("tap:stream:mystream.http.0", sourceChannelNode.getChannelName());
	}

	@Test
	public void sourceTapChannel3() {
		parse("mystream = http | file");
		StreamNode ast = null;
		SourceChannelNode sourceChannelNode = null;

		ast = parse("tap:stream:mystream.http > file");
		sourceChannelNode = ast.getSourceChannelNode();
		assertEquals("tap:stream:mystream.http.0", sourceChannelNode.getChannelName());
		assertEquals(ChannelType.TAP_STREAM, sourceChannelNode.getChannelType());

		ast = parse("tap:stream:mystream > file");
		sourceChannelNode = ast.getSourceChannelNode();
		// After resolution the name has been properly setup
		assertEquals("tap:stream:mystream.http.0", sourceChannelNode.getChannelName());
		assertEquals(ChannelType.TAP_STREAM, sourceChannelNode.getChannelType());
	}

	@Test
	public void nameSpaceTestWithSpaces() {
		checkForParseError("trigger > queue:job:myjob   too", XDDSLMessages.UNEXPECTED_DATA_AFTER_STREAMDEF, 28, "too");
	}

	@Test
	public void errorCases01() {
		checkForParseError(".", XDDSLMessages.EXPECTED_MODULENAME, 0, ".");
		checkForParseError(";", XDDSLMessages.EXPECTED_MODULENAME, 0, ";");
	}

	@Test
	public void errorCases04() {
		checkForParseError("foo bar=yyy", XDDSLMessages.UNEXPECTED_DATA_AFTER_STREAMDEF, 4, "bar");
		checkForParseError("foo bar", XDDSLMessages.UNEXPECTED_DATA_AFTER_STREAMDEF, 4, "bar");
	}

	@Test
	public void errorCases05() {
		checkForParseError("foo --", XDDSLMessages.OOD, 6);
		checkForParseError("foo --bar", XDDSLMessages.OOD, 9);
		checkForParseError("foo --bar=", XDDSLMessages.OOD, 10);
	}

	@Test
	public void errorCases06() {
		checkForParseError("|", XDDSLMessages.EXPECTED_MODULENAME, 0);
	}

	@Test
	public void errorCases07() {
		checkForParseError("foo > bar", XDDSLMessages.EXPECTED_CHANNEL_PREFIX_QUEUE_TOPIC, 6, "bar");
		checkForParseError("foo >", XDDSLMessages.OOD, 5);
		checkForParseError("foo > --2323", XDDSLMessages.EXPECTED_CHANNEL_PREFIX_QUEUE_TOPIC, 6, "--");
		checkForParseError("foo > *", XDDSLMessages.UNEXPECTED_DATA, 6, "*");
	}

	@Test
	public void errorCases08() {
		checkForParseError(":foo | bar", XDDSLMessages.EXPECTED_MODULENAME, 0, ":");
	}

	@Test
	public void errorCases09() {
		checkForParseError("* = http | file", XDDSLMessages.UNEXPECTED_DATA, 0, "*");
		checkForParseError(": = http | file", XDDSLMessages.ILLEGAL_STREAM_NAME, 0, ":");
	}

	@Test
	public void errorCase10() {
		checkForParseError("trigger > :job:foo", XDDSLMessages.EXPECTED_CHANNEL_PREFIX_QUEUE_TOPIC, 10, ":");
	}

	@Test
	public void errorCase11() {
		checkForParseError("tap:banana:yyy > file", XDDSLMessages.NOT_ALLOWED_TO_TAP_THAT, 4, "banana");
		checkForParseError("tap:xxx > file", XDDSLMessages.TAP_NEEDS_THREE_COMPONENTS, 0);
	}

	@Test
	public void duplicateExplicitLabels() {
		checkForParseError("xxx: http | xxx: file", XDDSLMessages.DUPLICATE_LABEL, 12, "xxx", "http", 0, "file", 1);
		checkForParseError("xxx: http | yyy: filter | transform | xxx: transform | file",
				XDDSLMessages.DUPLICATE_LABEL, 38, "xxx", "http", 0, "transform", 3);
		checkForParseError("xxx: http | yyy: filter | transform | xxx: transform | xxx: file",
				XDDSLMessages.DUPLICATE_LABEL, 38, "xxx", "http", 0, "transform", 3);
	}

	@Test
	public void addingALabelLiftsAmbiguity() {
		StreamNode ast = parse("file | out: file");
		assertEquals("file", ast.getModuleNodes().get(0).getLabelName());
		assertEquals("out", ast.getModuleNodes().get(1).getLabelName());

	}

	@Test
	public void duplicateImplicitLabels() {
		checkForParseError("http | filter | transform | transform | file",
				XDDSLMessages.DUPLICATE_LABEL, 28, "transform", "transform", 2, "transform", 3);
	}

	@Test
	public void tapWithLabels() {
		parse("mystream = http | flibble: transform | file");
		sn = parse("tap:stream:mystream.flibble > file");
		assertEquals("tap:stream:mystream.transform.1", sn.getSourceChannelNode().getChannelName());
	}

	@Test
	public void bridge01() {
		StreamNode sn = parse("queue:bar > topic:boo");
		assertEquals("[(queue:bar:0>9)>(ModuleNode:bridge:10>11)>(topic:boo:12>21)]", sn.stringify(true));
	}

	// Parameters must be constructed via adjacent tokens
	@Test
	public void needAdjacentTokensForParameters() {
		checkForParseError("foo -- name=value", XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_NAME, 7);
		checkForParseError("foo --name =value", XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_EQUALS, 11);
		checkForParseError("foo --name= value", XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_VALUE, 12);
	}

	// ---

	@Test
	public void testComposedOptionNameErros() {
		checkForParseError("foo --name.=value", XDDSLMessages.NOT_EXPECTED_TOKEN, 11);
		checkForParseError("foo --name .sub=value", XDDSLMessages.NO_WHITESPACE_IN_DOTTED_NAME, 11);
		checkForParseError("foo --name. sub=value", XDDSLMessages.NO_WHITESPACE_IN_DOTTED_NAME, 12);
	}

	@Test
	public void testXD2416() {
		StreamNode ast = parse("http | transform --expression='payload.replace(\"abc\", \"\")' | log");
		assertThat((String)ast.getModuleNodes().get(1).getArgumentsAsProperties().get("expression"), equalTo("payload.replace(\"abc\", \"\")"));

		ast = parse("http | transform --expression='payload.replace(\"abc\", '''')' | log");
		assertThat((String)ast.getModuleNodes().get(1).getArgumentsAsProperties().get("expression"), equalTo("payload.replace(\"abc\", '')"));
	}


	@After
	public void reset() {
		testRepository.reset();
	}

	private StreamConfigParser getParser() {
		return new StreamConfigParser(testRepository);
	}

	StreamNode parse(String streamDefinition) {
		StreamNode streamNode = getParser().parse(streamDefinition);
		if (streamNode.getStreamName() != null) {
			testRepository.save(new StreamDefinition(streamNode.getStreamName(), streamNode.getStreamData()));
		}
		return streamNode;
	}

	StreamNode parse(String streamName, String streamDefinition) {
		StreamNode streamNode = getParser().parse(streamName, streamDefinition);
		String sname = streamNode.getStreamName();
		if (sname == null) {
			sname = streamName;
		}
		if (sname != null) {
			testRepository.save(new StreamDefinition(sname, streamNode.getStreamData()));
		}
		return streamNode;
	}

	private void checkForIllegalStreamName(String streamName, String streamDef) {
		try {
			StreamNode sn = parse(streamName, streamDef);
			fail("expected to fail but parsed " + sn.stringify());
		}
		catch (StreamDefinitionException e) {
			assertEquals(XDDSLMessages.ILLEGAL_STREAM_NAME, e.getMessageCode());
			assertEquals(0, e.getPosition());
			assertEquals(streamName, e.getInserts()[0]);
		}
	}

	private void checkForParseError(String stream, XDDSLMessages msg, int pos, Object... inserts) {
		try {
			StreamNode sn = parse(stream);
			fail("expected to fail but parsed " + sn.stringify());
		}
		catch (StreamDefinitionException e) {
			assertEquals(msg, e.getMessageCode());
			assertEquals(pos, e.getPosition());
			if (inserts != null) {
				for (int i = 0; i < inserts.length; i++) {
					assertEquals(inserts[i], e.getInserts()[i]);
				}
			}
		}
	}

	private void checkForParseError(String name, String stream, XDDSLMessages msg, int pos, String... inserts) {
		try {
			StreamNode sn = parse(name, stream);
			fail("expected to fail but parsed " + sn.stringify());
		}
		catch (StreamDefinitionException e) {
			assertEquals(msg, e.getMessageCode());
			assertEquals(pos, e.getPosition());
			if (inserts != null) {
				for (int i = 0; i < inserts.length; i++) {
					assertEquals(inserts[i], e.getInserts()[i]);
				}
			}
		}
	}

	private static class TestRepository implements CrudRepository<StreamDefinition, String> {

		private final static boolean debugRepository = false;

		private Map<String, StreamDefinition> data = new HashMap<String, StreamDefinition>();

		public void reset() {
			data.clear();
		}

		@Override
		public <S extends StreamDefinition> S save(S entity) {
			if (debugRepository) {
				System.out.println(System.identityHashCode(this) + " save(" + entity + ")");
			}
			data.put(entity.getName(), entity);
			return entity;
		}

		@Override
		public <S extends StreamDefinition> Iterable<S> save(Iterable<S> entities) {
			throw new IllegalStateException();
		}

		@Override
		public StreamDefinition findOne(String id) {
			StreamDefinition sd = data.get(id);
			if (debugRepository) {
				System.out.println(System.identityHashCode(this) + " repository findOne(" + id + ") returning " + sd);
			}
			return sd;
		}

		@Override
		public boolean exists(String id) {
			throw new IllegalStateException();
		}

		@Override
		public Iterable<StreamDefinition> findAll() {
			return data.values();
		}

		@Override
		public Iterable<StreamDefinition> findAll(Iterable<String> ids) {
			throw new IllegalStateException();
		}

		@Override
		public long count() {
			throw new IllegalStateException();
		}

		@Override
		public void delete(String id) {
			throw new IllegalStateException();
		}

		@Override
		public void delete(StreamDefinition entity) {
			throw new IllegalStateException();
		}

		@Override
		public void delete(Iterable<? extends StreamDefinition> entities) {
			throw new IllegalStateException();
		}

		@Override
		public void deleteAll() {
			throw new IllegalStateException();
		}

	}
}
