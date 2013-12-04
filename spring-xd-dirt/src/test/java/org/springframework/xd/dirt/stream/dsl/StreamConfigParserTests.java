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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Test;

import org.springframework.core.io.Resource;
import org.springframework.data.repository.CrudRepository;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDependencyRepository;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.module.memory.InMemoryModuleDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.XDStreamParser;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

/**
 * Parse streams and verify either the correct abstract syntax tree is produced or the current exception comes out.
 * 
 * @author Andy Clement
 */
public class StreamConfigParserTests {

	private StreamNode sn;

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
	public void moduleLabels2() {
		StreamNode ast = parse("label: label2: http | label3: foo");
		assertEquals(
				"[((Label:label:0>5) (Label:label2:7>13) ModuleNode:http:0>19)((Label:label3:22>28) ModuleNode:foo:22>33)]",
				ast.stringify(true));
	}

	@Test
	public void moduleLabels3() {
		StreamNode ast = parse("food = http | label3: foo");
		assertEquals(
				"[food = (ModuleNode:http:7>11)((Label:label3:14>20) ModuleNode:foo:14>25)]",
				ast.stringify(true));

		sn = parse("http | foo:bar | file");
		assertEquals("[(ModuleNode:http)((Label:foo) ModuleNode:bar)(ModuleNode:file)]", sn.stringify());

		sn = parse("http | foo: goggle: bar | file");
		assertEquals("[(ModuleNode:http)((Label:foo) (Label:goggle) ModuleNode:bar)(ModuleNode:file)]", sn.stringify());

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
		XDStreamParser parser = new XDStreamParser(testRepository, moduleDefinitionRepository());
		try {
			parser.parse("t", config);
			fail(config + " is invalid. Should throw exception");
		}
		catch (Exception e) {
			// success
		}
	}

	public ModuleDefinitionRepository moduleDefinitionRepository() {
		ModuleRegistry registry = mock(ModuleRegistry.class);
		ModuleDependencyRepository moduleDependencyRepository = mock(ModuleDependencyRepository.class);
		Resource resource = mock(Resource.class);
		File file = mock(File.class);
		when(file.exists()).thenReturn(true);
		try {
			when(resource.getFile()).thenReturn(file);
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		ArrayList<ModuleDefinition> definitions = new ArrayList<ModuleDefinition>();
		definitions.add(new ModuleDefinition("SOURCE",
				ModuleType.source, resource));
		when(registry.findDefinitions("SOURCE")).thenReturn(
				definitions);

		definitions = new ArrayList<ModuleDefinition>();
		definitions.add(new ModuleDefinition("SINK",
				ModuleType.sink, resource));
		when(registry.findDefinitions("SINK")).thenReturn(
				definitions);

		definitions = new ArrayList<ModuleDefinition>();
		definitions.add(new ModuleDefinition("PROCESSOR",
				ModuleType.processor, resource));
		when(registry.findDefinitions("PROCESSOR")).thenReturn(
				definitions);
		return new InMemoryModuleDefinitionRepository(registry, moduleDependencyRepository);
	}

	@Test
	public void tapWithLabelReference() {
		parse("mystream = http | filter | group1: transform | group2: transform | file");
		StreamNode ast = parse("tap:stream:mystream.group1 > file");
		// post resolution 'group1' is transformed to transform
		assertEquals("[(tap:stream:mystream.transform)>(ModuleNode:file)]", ast.stringify());
		ast = parse("tap:stream:mystream > file");
		assertEquals("[(tap:stream:mystream.http)>(ModuleNode:file)]", ast.stringify());
	}

	@Test
	public void tapWithQualifiedModuleReference() {
		parse("mystream = http | foobar | file");
		StreamNode sn = parse("tap:stream:mystream.foobar > file");
		assertEquals("[(tap:stream:mystream.foobar:0>26)>(ModuleNode:file:29>33)]", sn.stringify(true));
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
	public void expressions_xd159_3() {
		StreamNode ast = parse("foo |  transform --expression='new StringBuilder(payload).reverse()' | bar");
		ModuleNode mn = ast.getModule("transform");
		Properties props = mn.getArgumentsAsProperties();
		assertEquals("new StringBuilder(payload).reverse()", props.get("expression"));
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
		checkForParseError("foo |  transform --expression=''Hello, world!'' | bar", XDDSLMessages.UNEXPECTED_DATA, 37);
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

		checkForParseError("tap:queue:boo.xx.yy > file", XDDSLMessages.ONLY_A_TAP_ON_A_STREAM_CAN_BE_INDEXED, 13);

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
		assertEquals("[(tap:stream:foo.bar:0>18)>(ModuleNode:file:21>25)]", ast.stringify(true));
		assertEquals("tap:foo.bar", ast.getSourceChannelNode().getChannelName());
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
				"[(tap:stream:mystream.http:0>24)>(ModuleNode:file:27>31)]",
				ast.stringify(true));
	}

	@Test
	public void sourceTapChannelNoColon() {
		parse("mystream = http | file");
		StreamNode ast = null;
		SourceChannelNode sourceChannelNode = null;

		ast = parse("tap:stream:mystream.http > file");
		sourceChannelNode = ast.getSourceChannelNode();
		assertEquals("tap:mystream.http", sourceChannelNode.getChannelName());
	}

	@Test
	public void sourceTapChannel3() {
		parse("mystream = http | file");
		StreamNode ast = null;
		SourceChannelNode sourceChannelNode = null;

		ast = parse("tap:stream:mystream.http > file");
		sourceChannelNode = ast.getSourceChannelNode();
		assertEquals("tap:mystream.http", sourceChannelNode.getChannelName());
		assertEquals(ChannelType.TAP_STREAM, sourceChannelNode.getChannelType());

		ast = parse("tap:stream:mystream > file");
		sourceChannelNode = ast.getSourceChannelNode();
		// After resolution the name has been properly setup
		assertEquals("tap:mystream.http", sourceChannelNode.getChannelName());
		assertEquals(ChannelType.TAP_STREAM, sourceChannelNode.getChannelType());
	}

	@Test
	public void substreams() {
		parse("myhttp = http --port=9000");
		StreamNode stream = parse("myhttp | file");
		assertEquals("[(ModuleNode:http --port=9000:9>25)(ModuleNode:file:9>13)]",
				stream.stringify(true));
	}

	@Test
	public void substreamsWithSourceChannels() {
		parse("myhttp = queue:foo > filter --name=payload");

		sn = parse("myhttp | file");

		assertEquals("[(queue:foo:9>18)>(ModuleNode:filter --name=payload:21>42)(ModuleNode:file:9>13)]",
				sn.stringify(true));

		checkForParseError("queue:foo > myhttp | file",
				XDDSLMessages.CANNOT_USE_COMPOSEDMODULE_HERE_ALREADY_HAS_SOURCE_CHANNEL, 12, "myhttp");

		checkForParseError("foo | myhttp | file",
				XDDSLMessages.CANNOT_USE_COMPOSEDMODULE_HERE_AS_IT_DEFINES_SOURCE_CHANNEL, 6, "myhttp");

		sn = parse("myhttp > topic:wibble");
		assertEquals("[(queue:foo)>(ModuleNode:filter --name=payload)>(topic:wibble)]", sn.stringify());
	}

	@Test
	public void substreamsWithSinkChannels() {
		checkForParseError("myhttp = filter > :foo", XDDSLMessages.EXPECTED_CHANNEL_PREFIX_QUEUE_TOPIC, 18, ":");

		parse("mysink = filter --payload=true > queue:foo");

		sn = parse("http | mysink");
		assertEquals("[(ModuleNode:http:0>4)(ModuleNode:filter --payload=true:9>30)>(queue:foo:33>42)]",
				sn.stringify(true));

		checkForParseError("http | mysink > topic:bar",
				XDDSLMessages.CANNOT_USE_COMPOSEDMODULE_HERE_ALREADY_HAS_SINK_CHANNEL, 7, "mysink");

		checkForParseError("foo | mysink | file",
				XDDSLMessages.CANNOT_USE_COMPOSEDMODULE_HERE_AS_IT_DEFINES_SINK_CHANNEL, 6, "mysink");

		sn = parse("myhttp > topic:wibble");
		assertEquals("[(ModuleNode:myhttp)>(topic:wibble)]", sn.stringify());
	}

	@Test
	public void substreamsMultipleModules() {
		parse("foo = transform --expression='abc' | transform --expression='def'");
		StreamNode stream = parse("http | foo | file");
		// TODO after macro insertion the source locations for the inserted modules are
		// kind of meaningless, reset them?
		assertEquals(
				"[(ModuleNode:http:0>4)(ModuleNode:transform --expression=abc:6>32)(ModuleNode:transform --expression=def:35>61)(ModuleNode:file:13>17)]",
				stream.stringify(true));
	}

	@Test
	public void substreamsAdditionalParams() {
		parse("myhttp = http --port=9000");
		StreamNode stream = parse("myhttp --setting2=value2 | file");
		assertEquals("[(ModuleNode:http --port=9000 --setting2=value2)(ModuleNode:file)]", stream.stringify());
	}

	@Test
	public void substreamsOverrideParams() {
		parse("myhttp = http --port=9000");
		StreamNode stream = parse("myhttp --port=9010| file");
		assertEquals("[(ModuleNode:http --port=9010)(ModuleNode:file)]", stream.stringify());
	}

	@Test
	public void parameterizedStreams() {
		parse("nameReplacer = transform --expression=payload.replaceAll('${name}','x')");
		StreamNode stream = parse("http | nameReplacer --name='Andy' | file");
		assertEquals(
				"[(ModuleNode:http)(ModuleNode:transform --expression=payload.replaceAll('Andy','x'))(ModuleNode:file)]",
				stream.stringify());
	}

	@Test
	public void parameterizedStreamsMissingValue() {
		parse("nameReplacer = transform --expression=payload.replaceAll('${name}','x')");
		checkForParseError("http | nameReplacer --name2='Andy' | file", XDDSLMessages.MISSING_VALUE_FOR_VARIABLE, -1,
				"name");
	}

	@Test
	public void parameterizedStreamsMissingCloseCurly() {
		parse("nameReplacer = transform --expression=payload.replaceAll('${name','x')");
		checkForParseError("http | nameReplacer --name2='Andy' | file", XDDSLMessages.VARIABLE_NOT_TERMINATED, -1,
				"--expression=payload.replaceAll('${name','x')");
	}

	@Test
	public void parameterizedStreamsDefaultValues() {
		parse("nameReplacer = transform --expression=payload.replaceAll('${name:foo}','x')");
		StreamNode stream = parse("http | nameReplacer | file");
		assertEquals(
				"[(ModuleNode:http)(ModuleNode:transform --expression=payload.replaceAll('foo','x'))(ModuleNode:file)]",
				stream.stringify());
	}

	@Test
	public void parameterizedStreamsMixingItUp() {
		parse("nameReplacer = transform --expression=payload.replaceAll('${name:foo}','x')");
		StreamNode stream = parse("http | nameReplacer --setting2=value2 | file");
		assertEquals(
				"[(ModuleNode:http)(ModuleNode:transform --expression=payload.replaceAll('foo','x') --setting2=value2)(ModuleNode:file)]",
				stream.stringify());
	}

	@Test
	public void parameterizedStreamsMixingItUp2() {
		parse("nameReplacer = transform --a1=${foo} --b1=${bar}");
		StreamNode stream = parse("http | nameReplacer --foo=abc --bar=def | file");
		assertEquals("[(ModuleNode:http)(ModuleNode:transform --a1=abc --b1=def)(ModuleNode:file)]",
				stream.stringify());
	}

	@Test
	public void parameterizedSubstreamsMultipleModules() {
		parse("nameReplacer = transform --a1=${foo} | transform --b1=${bar}");
		StreamNode stream = parse("http | nameReplacer --foo=abc --bar=def | file");
		assertEquals(
				"[(ModuleNode:http)(ModuleNode:transform --a1=abc)(ModuleNode:transform --b1=def)(ModuleNode:file)]",
				stream.stringify());
	}

	@Test
	public void parameterizedSubstreamsMultipleModulesDefaultValues() {
		parse("nameReplacer = transform --a1=${foo:default} | transform --b1=${bar}");
		StreamNode stream = parse("http | nameReplacer --bar=def | file");
		assertEquals(
				"[(ModuleNode:http)(ModuleNode:transform --a1=default)(ModuleNode:transform --b1=def)(ModuleNode:file)]",
				stream.stringify());
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
	public void errorCases02() {
		// If we allow hyphens in identifiers (stream names) then this is not invalid, it
		// is a stream called 'foo--bar'
		// checkForParseError("foo--bar=yyy",XDDSLMessages.EXPECTED_WHITESPACE_AFTER_MODULE_BEFORE_ARGUMENT,3);
		StreamNode ast = parse("foo--bar=yyy");
		assertEquals("[foo--bar = (ModuleNode:yyy)]", ast.stringify());
	}

	@Test
	public void errorCases03() {
		// If we allow hyphens in identifiers (stream names) then this is not invalid, it
		// is a stream called 'foo--bar'
		// checkForParseError("foo-bar=yyy",XDDSLMessages.MISSING_CHARACTER,3,"-");
		StreamNode ast = parse("foo-bar=yyy");
		assertEquals("[foo-bar = (ModuleNode:yyy)]", ast.stringify());
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
	public void bridge01() {
		StreamNode sn = parse("queue:bar > topic:boo");
		assertEquals("[(queue:bar:0>9)>(ModuleNode:bridge:10>11)>(topic:boo:12>21)]", sn.stringify(true));
	}

	// Parameters must be constructed via adjacent tokens
	@Test
	public void needAdjacentTokens() {
		checkForParseError("foo -- name=value", XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_NAME, 7);
		checkForParseError("foo --name =value", XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_EQUALS, 11);
		checkForParseError("foo --name= value", XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_VALUE, 12);
	}

	// ---

	private TestRepository testRepository = new TestRepository();

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

	private void checkForParseError(String stream, XDDSLMessages msg, int pos, String... inserts) {
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
