/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.x.channel.registry;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.channel.DirectChannel;

/**
 * @author Gary Russell
 * @since 1.0
 *
 */
public class LocalChannelRegistryTests {

	@Test
	public void test() throws Exception {
		LocalChannelRegistry registry = new LocalChannelRegistry();
		registry.setApplicationContext(new GenericApplicationContext());
		registry.afterPropertiesSet();
		registry.outbound("foo.0", new DirectChannel());
		registry.inbound("foo.0", new DirectChannel());
		registry.outbound("foo.1", new DirectChannel());
		registry.inbound("foo.1", new DirectChannel());
		registry.outbound("foo.2", new DirectChannel());
		registry.tap("foo.0", new DirectChannel());
		DirectFieldAccessor accessor = new DirectFieldAccessor(registry);
		List<?> bridges = (List<?>) accessor.getPropertyValue("bridges");
		assertEquals(6, bridges.size());
		registry.cleanAll("foo.0");
		assertEquals(4, bridges.size());
		registry.cleanAll("foo.1");
		assertEquals(2, bridges.size());
		registry.cleanAll("foo.2");
		assertEquals(1, bridges.size()); // tap remains
	}

}
