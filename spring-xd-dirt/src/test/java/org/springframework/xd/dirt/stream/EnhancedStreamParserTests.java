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

package org.springframework.xd.dirt.stream;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;

/**
 * @author Mark Fisher
 * @author David Turanski
 */
public class EnhancedStreamParserTests {

	private EnhancedStreamParser parser;

	@Before
	public void setup() {
		parser = new EnhancedStreamParser();
	}

	@Test
	public void testJob() {
		List<ModuleDeploymentRequest> requests = parser.parse("myJob", "job");
		assertEquals(1, requests.size());
		ModuleDeploymentRequest job = requests.get(0);
		assertEquals("job", job.getModule());
		assertEquals("myJob", job.getGroup());
		assertEquals(0, job.getIndex());
		assertEquals("job", job.getType());
		assertEquals(0, job.getParameters().size());
	}

	@Test
	public void testJobWithParams() {
		List<ModuleDeploymentRequest> requests = parser.parse("myJob", "job --foo=bar");
		assertEquals(1, requests.size());
		ModuleDeploymentRequest job = requests.get(0);
		assertEquals("job", job.getModule());
		assertEquals("myJob", job.getGroup());
		assertEquals(0, job.getIndex());
		assertEquals("job", job.getType());
		assertEquals(1, job.getParameters().size());
		assertEquals("bar", job.getParameters().get("foo"));
	}

	@Test
	public void simpleStream() {
		List<ModuleDeploymentRequest> requests = parser.parse("test", "foo | bar");
		assertEquals(2, requests.size());
		ModuleDeploymentRequest sink = requests.get(0);
		ModuleDeploymentRequest source = requests.get(1);
		assertEquals("foo", source.getModule());
		assertEquals("test", source.getGroup());
		assertEquals(0, source.getIndex());
		assertEquals("source", source.getType());
		assertEquals(0, source.getParameters().size());
		assertEquals("bar", sink.getModule());
		assertEquals("test", sink.getGroup());
		assertEquals(1, sink.getIndex());
		assertEquals("sink", sink.getType());
		assertEquals(0, sink.getParameters().size());
	}

	@Test
	public void quotesInParams() {
		List<ModuleDeploymentRequest> requests = parser.parse("test", "foo --bar='payload.matches(''hello'')' | file");
		assertEquals(2, requests.size());
		//		ModuleDeploymentRequest sink = requests.get(0);
		ModuleDeploymentRequest source = requests.get(1);
		assertEquals("foo", source.getModule());
		assertEquals("test", source.getGroup());
		assertEquals(0, source.getIndex());
		assertEquals("source", source.getType());
		Map<String, String> sourceParameters = source.getParameters();
		assertEquals(1, sourceParameters.size());
		assertEquals("payload.matches('hello')", sourceParameters.get("bar"));
	}

	@Test
	public void quotesInParams2() {
		List<ModuleDeploymentRequest> requests = parser.parse("test", "http --port=9700 | filter --expression=payload.matches('hello world') | file");
		assertEquals(3, requests.size());
		ModuleDeploymentRequest filter = requests.get(1);
		assertEquals("filter", filter.getModule());
		assertEquals("test", filter.getGroup());
		assertEquals(1, filter.getIndex());
		assertEquals("processor", filter.getType());
		Map<String, String> sourceParameters = filter.getParameters();
		assertEquals(1, sourceParameters.size());
		assertEquals("payload.matches('hello world')", sourceParameters.get("expression"));
	}

	@Test
	public void parameterizedModules() {
		List<ModuleDeploymentRequest> requests = parser.parse("test", "foo --x=1 --y=two | bar --z=3");
		assertEquals(2, requests.size());
		ModuleDeploymentRequest sink = requests.get(0);
		ModuleDeploymentRequest source = requests.get(1);
		assertEquals("foo", source.getModule());
		assertEquals("test", source.getGroup());
		assertEquals(0, source.getIndex());
		assertEquals("source", source.getType());
		Map<String, String> sourceParameters = source.getParameters();
		assertEquals(2, sourceParameters.size());
		assertEquals("1", sourceParameters.get("x"));
		assertEquals("two", sourceParameters.get("y"));
		assertEquals("bar", sink.getModule());
		assertEquals("test", sink.getGroup());
		assertEquals(1, sink.getIndex());
		assertEquals("sink", sink.getType());
		Map<String, String> sinkParameters = sink.getParameters();
		assertEquals(1, sinkParameters.size());
		assertEquals("3", sinkParameters.get("z"));
	}

}