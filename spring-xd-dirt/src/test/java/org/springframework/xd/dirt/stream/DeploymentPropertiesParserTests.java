/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.stream;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.rest.domain.support.DeploymentPropertiesFormat;

/**
 * Tests for the {@link ModuleDeploymentProperties} parser.
 *
 * @author Mark Fisher
 */
public class DeploymentPropertiesParserTests {

	@Test
	public void singleProperty() {
		Map<String, String> map = testDeploymentPropertiesParser("module.http.count=99");
		assertEquals(1, map.size());
		assertEquals("99", map.get("module.http.count"));
	}

	@Test
	public void simpleProperties() {
		Map<String, String> map = testDeploymentPropertiesParser("module.foo.x=1, module.bar.y=2");
		assertEquals(2, map.size());
		assertEquals("1", map.get("module.foo.x"));
		assertEquals("2", map.get("module.bar.y"));
	}

	@Test
	public void crazyWhitespace() {
		Map<String, String> map = testDeploymentPropertiesParser("  module.a.b =   123 \t ,  \n  module.c.d =  trimme ");
		assertEquals(2, map.size());
		assertEquals("123", map.get("module.a.b"));
		assertEquals("trimme", map.get("module.c.d"));
	}

	@Test
	public void expressionsIncludingCommasInValue() {
		Map<String, String> map = testDeploymentPropertiesParser(
				"module.time.count=3, module.x.greeting='hi, ' + ${name}, module.x.criteria=groups.contains('foo')");
		assertEquals(3, map.size());
		assertEquals("3", map.get("module.time.count"));
		assertEquals("'hi, ' + ${name}", map.get("module.x.greeting"));
		assertEquals("groups.contains('foo')", map.get("module.x.criteria"));
	}

	private static Map<String, String> testDeploymentPropertiesParser(String s) {
		return DeploymentPropertiesFormat.parseDeploymentProperties(s);
	}

}
