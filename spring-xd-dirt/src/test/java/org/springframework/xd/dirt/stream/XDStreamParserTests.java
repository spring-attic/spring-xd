/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.stream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.xd.dirt.stream.ParsingContext.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.stream.dsl.StreamDefinitionException;
import org.springframework.xd.dirt.zookeeper.EmbeddedZooKeeper;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.TestModuleDefinitions;
import org.springframework.xd.module.options.DefaultModuleOptionsMetadataResolver;

/**
 * @author Mark Fisher
 * @author David Turanski
 */
public class XDStreamParserTests {

	private XDStreamParser parser;

	@Before
	public void setup() {
		parser = new XDStreamParser(moduleRegistry(), new DefaultModuleOptionsMetadataResolver());
	}

	@Test
	public void testJob() {
		List<ModuleDescriptor> requests = parser.parse("myJob", "job", job);
		assertEquals(1, requests.size());
		ModuleDescriptor job = requests.get(0);
		assertEquals("job", job.getModuleName());
		assertEquals("myJob", job.getGroup());
		assertEquals(0, job.getIndex());
		assertEquals(ModuleType.job, job.getType());
		assertEquals(0, job.getParameters().size());
	}

	@Test
	public void testJobWithParams() {
		List<ModuleDescriptor> requests = parser.parse("myJob", "job --foo=bar", job);
		assertEquals(1, requests.size());
		ModuleDescriptor job = requests.get(0);
		assertEquals("job", job.getModuleName());
		assertEquals("myJob", job.getGroup());
		assertEquals(0, job.getIndex());
		assertEquals(ModuleType.job, job.getType());
		assertEquals(1, job.getParameters().size());
		assertEquals("bar", job.getParameters().get("foo"));
	}

	@Test
	public void simpleStream() {
		List<ModuleDescriptor> requests = parser.parse("test", "foo | bar", stream);
		assertEquals(2, requests.size());
		ModuleDescriptor sink = requests.get(0);
		ModuleDescriptor source = requests.get(1);
		assertEquals("foo", source.getModuleName());
		assertEquals("test", source.getGroup());
		assertEquals(0, source.getIndex());
		assertEquals(ModuleType.source, source.getType());
		assertEquals(0, source.getParameters().size());
		assertEquals("bar", sink.getModuleName());
		assertEquals("test", sink.getGroup());
		assertEquals(1, sink.getIndex());
		assertEquals(ModuleType.sink, sink.getType());
		assertEquals(0, sink.getParameters().size());
	}

	@Test
	public void quotesInParams() {
		List<ModuleDescriptor> requests = parser.parse("test", "foo --bar='payload.matches(''hello'')' | file",
				stream);
		assertEquals(2, requests.size());
		// ModuleDescriptor sink = requests.get(0);
		ModuleDescriptor source = requests.get(1);
		assertEquals("foo", source.getModuleName());
		assertEquals("test", source.getGroup());
		assertEquals(0, source.getIndex());
		assertEquals(ModuleType.source, source.getType());
		Map<String, String> sourceParameters = source.getParameters();
		assertEquals(1, sourceParameters.size());
		assertEquals("payload.matches('hello')", sourceParameters.get("bar"));
	}

	@Test
	public void quotesInParams2() {
		List<ModuleDescriptor> requests = parser.parse("test",
				"http --port=9700 | filter --expression=payload.matches('hello world') | file", stream);
		assertEquals(3, requests.size());
		ModuleDescriptor filter = requests.get(1);
		assertEquals("filter", filter.getModuleName());
		assertEquals("test", filter.getGroup());
		assertEquals(1, filter.getIndex());
		assertEquals(ModuleType.processor, filter.getType());
		Map<String, String> sourceParameters = filter.getParameters();
		assertEquals(1, sourceParameters.size());
		assertEquals("payload.matches('hello world')", sourceParameters.get("expression"));
	}

	@Test
	public void parameterizedModules() {
		List<ModuleDescriptor> requests = parser.parse("test", "foo --x=1 --y=two | bar --z=3", stream);
		assertEquals(2, requests.size());
		ModuleDescriptor sink = requests.get(0);
		ModuleDescriptor source = requests.get(1);
		assertEquals("foo", source.getModuleName());
		assertEquals("test", source.getGroup());
		assertEquals(0, source.getIndex());
		assertEquals(ModuleType.source, source.getType());
		Map<String, String> sourceParameters = source.getParameters();
		assertEquals(2, sourceParameters.size());
		assertEquals("1", sourceParameters.get("x"));
		assertEquals("two", sourceParameters.get("y"));
		assertEquals("bar", sink.getModuleName());
		assertEquals("test", sink.getGroup());
		assertEquals(1, sink.getIndex());
		assertEquals(ModuleType.sink, sink.getType());
		Map<String, String> sinkParameters = sink.getParameters();
		assertEquals(1, sinkParameters.size());
		assertEquals("3", sinkParameters.get("z"));
	}

	@Test
	public void sourceChannelNameIsAppliedToSourceModule() throws Exception {
		List<ModuleDescriptor> requests = parser.parse("test", "topic:foo > goo | blah | file", stream);
		assertEquals(3, requests.size());
		assertEquals("topic:foo", requests.get(2).getSourceChannelName());
		assertEquals(ModuleType.processor, requests.get(2).getType());
		assertEquals(ModuleType.processor, requests.get(1).getType());
		assertEquals(ModuleType.sink, requests.get(0).getType());
	}

	@Test
	public void sinkChannelNameIsAppliedToSinkModule() throws Exception {
		List<ModuleDescriptor> requests = parser.parse("test", "boo | blah | aaak > queue:foo", stream);
		assertEquals(3, requests.size());
		assertEquals("queue:foo", requests.get(0).getSinkChannelName());
		assertEquals(ModuleType.processor, requests.get(0).getType());
		assertEquals(ModuleType.processor, requests.get(1).getType());
		assertEquals(ModuleType.source, requests.get(2).getType());
	}

	@Test
	public void tap() throws Exception {
		StreamDefinitionRepository streamRepo = mock(StreamDefinitionRepository.class);
		parser = new XDStreamParser(streamRepo, moduleRegistry(),
				new DefaultModuleOptionsMetadataResolver());
		when(streamRepo.findOne("xxx")).thenReturn(new StreamDefinition("xxx", "http | file"));
		List<ModuleDescriptor> requests = parser.parse("test", "tap:stream:xxx.http > file", stream);
		assertEquals(1, requests.size());
		assertEquals("tap:stream:xxx.http.0", requests.get(0).getSourceChannelName());
		assertEquals(ModuleType.sink, requests.get(0).getType());
	}

	@Test
	public void simpleSinkNamedChannel() throws Exception {
		List<ModuleDescriptor> requests = parser.parse("test", "bart > queue:foo", stream);
		assertEquals(1, requests.size());
		assertEquals("queue:foo", requests.get(0).getSinkChannelName());
		assertEquals(ModuleType.source, requests.get(0).getType());
	}

	@Test
	public void simpleSinkNamedChannelBadType() throws Exception {
		// The parser will identify this as a Named channel sink and thus badLog will be
		// labeled a source.
		// But badLog is a sink and there should be an exception thrown by the parser.
		boolean isException = false;
		try {
			parser.parse("test", "badLog > :foo", stream);
		}
		catch (Exception e) {
			isException = true;
		}
		assertTrue(isException);
	}

	@Test
	public void simpleSourceNamedChannel() throws Exception {
		List<ModuleDescriptor> requests = parser.parse("test", "queue:foo > boot", stream);
		assertEquals(1, requests.size());
		assertEquals("queue:foo", requests.get(0).getSourceChannelName());
		assertEquals(ModuleType.sink, requests.get(0).getType());
	}

	@Test
	public void namedChannelsForbiddenInComposedModules() {
		try {
			parser.parse("test", "queue:foo > boot", module);
		}
		catch (StreamDefinitionException expected) {
			assertThat(expected.getMessage(),
					containsString("A named channel is not supported in this kind of definition"));
			assertThat(expected.getPosition(), is(0));
		}
		try {
			parser.parse("test", "bart | goo > queue:foo", module);
		}
		catch (StreamDefinitionException expected) {
			assertThat(expected.getMessage(),
					containsString("A named channel is not supported in this kind of definition"));
			assertThat(expected.getPosition(), is(13));
		}
	}

	@Bean
	public ZooKeeperConnection zooKeeperConnection() {
		return new ZooKeeperConnection("localhost:" + embeddedZooKeeper().getClientPort());
	}

	@Bean
	public EmbeddedZooKeeper embeddedZooKeeper() {
		return new EmbeddedZooKeeper();
	}

	@Bean
	public ModuleRegistry moduleRegistry() {
		ModuleRegistry registry = mock(ModuleRegistry.class);
		setupMockFindsForSource(registry);
		setupMockFindsForSink(registry);
		setupMockFindsForProcessor(registry);
		setupMockFindsForJobs(registry);

		when(registry.findDefinition("bart", ModuleType.source)).thenReturn(TestModuleDefinitions.dummy("bart",
				ModuleType.source));
		when(registry.findDefinition("foo", ModuleType.source)).thenReturn(TestModuleDefinitions.dummy("foo",
				ModuleType.source));
		when(registry.findDefinition("boo", ModuleType.source)).thenReturn(TestModuleDefinitions.dummy("boo",
				ModuleType.source));
		when(registry.findDefinition("http", ModuleType.source)).thenReturn(TestModuleDefinitions.dummy("http",
				ModuleType.source));

		when(registry.findDefinition("boot", ModuleType.sink)).thenReturn(TestModuleDefinitions.dummy("boot",
				ModuleType.sink));
		when(registry.findDefinition("bar", ModuleType.sink)).thenReturn(TestModuleDefinitions.dummy("bar",
				ModuleType.sink));
		when(registry.findDefinition("badLog", ModuleType.sink)).thenReturn(TestModuleDefinitions.dummy("badLog",
				ModuleType.sink));
		when(registry.findDefinition("file", ModuleType.sink)).thenReturn(TestModuleDefinitions.dummy("file",
				ModuleType.sink));

		when(registry.findDefinition("job", ModuleType.job)).thenReturn(TestModuleDefinitions.dummy("job",
				ModuleType.job));

		when(registry.findDefinition("aaak", ModuleType.processor)).thenReturn(TestModuleDefinitions.dummy("aaak",
				ModuleType.processor));
		when(registry.findDefinition("goo", ModuleType.processor)).thenReturn(TestModuleDefinitions.dummy("goo",
				ModuleType.processor));
		when(registry.findDefinition("blah", ModuleType.processor)).thenReturn(TestModuleDefinitions.dummy("blah",
				ModuleType.processor));
		when(registry.findDefinition("filter", ModuleType.processor)).thenReturn(TestModuleDefinitions.dummy("filter",
				ModuleType.processor));

		return registry;
	}

	private void setupMockFindsForSource(ModuleRegistry registry) {
		when(registry.findDefinitions("source")).thenReturn(Collections.singletonList(TestModuleDefinitions.dummy("source",
				ModuleType.source)));
		when(registry.findDefinitions("foo")).thenReturn(Collections.singletonList(TestModuleDefinitions.dummy("foo",
				ModuleType.source)));
		when(registry.findDefinitions("boo")).thenReturn(Collections.singletonList(TestModuleDefinitions.dummy("boo",
				ModuleType.source)));
		when(registry.findDefinitions("http")).thenReturn(Collections.singletonList(TestModuleDefinitions.dummy("http",
				ModuleType.source)));
	}

	private void setupMockFindsForSink(ModuleRegistry registry) {
		when(registry.findDefinitions("sink")).thenReturn(Collections.singletonList(TestModuleDefinitions.dummy("sink",
				ModuleType.sink)));
		when(registry.findDefinitions("file")).thenReturn(Collections.singletonList(TestModuleDefinitions.dummy("file",
				ModuleType.sink)));
		when(registry.findDefinitions("boot")).thenReturn(Collections.singletonList(TestModuleDefinitions.dummy("boot",
				ModuleType.sink)));
		when(registry.findDefinitions("bar")).thenReturn(Collections.singletonList(TestModuleDefinitions.dummy("bar",
				ModuleType.sink)));
	}

	private void setupMockFindsForProcessor(ModuleRegistry registry) {
		when(registry.findDefinitions("processor")).thenReturn(
				Collections.singletonList(TestModuleDefinitions.dummy("processor",
						ModuleType.processor)));
		when(registry.findDefinitions("blah")).thenReturn(Collections.singletonList(TestModuleDefinitions.dummy("blah",
				ModuleType.processor)));
		when(registry.findDefinitions("filter")).thenReturn(Collections.singletonList(TestModuleDefinitions.dummy("filter",
				ModuleType.processor)));
		when(registry.findDefinitions("goo")).thenReturn(Collections.singletonList(TestModuleDefinitions.dummy("goo",
				ModuleType.processor)));
		when(registry.findDefinitions("aaak")).thenReturn(Collections.singletonList(TestModuleDefinitions.dummy("aaak",
				ModuleType.processor)));
	}

	private void setupMockFindsForJobs(ModuleRegistry registry) {
		when(registry.findDefinitions("job")).thenReturn(Collections.singletonList(TestModuleDefinitions.dummy("job",
				ModuleType.job)));

	}

}
