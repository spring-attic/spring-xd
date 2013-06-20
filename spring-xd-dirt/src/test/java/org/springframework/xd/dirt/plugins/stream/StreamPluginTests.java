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

package org.springframework.xd.dirt.plugins;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.integration.x.channel.registry.LocalChannelRegistry;
import org.springframework.xd.module.Module;
import org.springframework.xd.module.SimpleModule;

/**
 * @author Mark Fisher
 */
public class StreamPluginTests {

	@Test
	public void streamNamePropertyAdded() {
		LocalChannelRegistry channelRegistry = new LocalChannelRegistry();
		StreamPlugin plugin = new StreamPlugin(channelRegistry);
		Module module = new SimpleModule("testsource", "source");
		assertEquals(0, module.getProperties().size());
		plugin.processModule(module, "foo", 0);
		assertEquals(1, module.getProperties().size());
		assertEquals("foo", module.getProperties().getProperty("xd.stream.name"));
	}

}
