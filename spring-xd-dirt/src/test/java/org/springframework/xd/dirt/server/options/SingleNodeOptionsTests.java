/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.xd.dirt.server.options;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


/**
 * 
 * @author David Turanski
 */
public class SingleNodeOptionsTests {

	@Test
	public void testSingleNodeOptionsWithOptions() {
		SingleNodeOptions opts = new SingleNodeOptions();
		new CommandLineParser(opts).parseArgument(new String[] { "--httpPort", "0", "--transport",
			"local", "--store", "memory", "--analytics", "memory", "--enableJmx", "false" });

		assertEquals(Store.memory, opts.getStore());
		assertEquals(Analytics.memory, opts.getAnalytics());
		assertEquals(false, opts.isJmxEnabled());
		assertEquals(0, (int) opts.getHttpPort());
		assertEquals(8778, (int) opts.getJmxPort());
		assertFalse(opts.isExplicit(opts.getXDHomeDir()));
		assertTrue(opts.isExplicit(opts.getTransport()));
		assertTrue(opts.isExplicit(opts.getAnalytics()));
		assertTrue(opts.isExplicit(opts.getStore()));
		assertFalse(opts.isExplicit(opts.getHadoopDistro()));
		assertTrue(opts.isExplicit(opts.getHttpPort()));
		assertFalse(opts.isExplicit(opts.getJmxPort()));
		assertTrue(opts.isExplicit(opts.isJmxEnabled()));

	}

	@Test
	public void testSingleNodeOptionsToContainerOptions() {
		SingleNodeOptions opts = new SingleNodeOptions();
		new CommandLineParser(opts).parseArgument(new String[] { "--httpPort", "0", "--transport",
			"local", "--store", "memory", "--analytics", "memory", "--enableJmx", "false" });

		assertEquals(Store.memory, opts.getStore());
		assertEquals(Analytics.memory, opts.getAnalytics());
		assertEquals(false, opts.isJmxEnabled());
		assertEquals(0, (int) opts.getHttpPort());
		assertEquals(8778, (int) opts.getJmxPort());
		assertFalse(opts.isExplicit(opts.getXDHomeDir()));
		assertTrue(opts.isExplicit(opts.getTransport()));
		assertTrue(opts.isExplicit(opts.getAnalytics()));
		assertTrue(opts.isExplicit(opts.getStore()));
		assertFalse(opts.isExplicit(opts.getHadoopDistro()));
		assertTrue(opts.isExplicit(opts.getHttpPort()));
		assertFalse(opts.isExplicit(opts.getJmxPort()));
		assertTrue(opts.isExplicit(opts.isJmxEnabled()));

		ContainerOptions containerOpts = opts.asContainerOptions();

		assertSame(containerOpts.getAnalytics(), opts.getAnalytics());
		assertSame(containerOpts.getXDHomeDir(), opts.getXDHomeDir());
		assertSame(containerOpts.getTransport(), opts.getTransport());
		assertSame(containerOpts.getHadoopDistro(), opts.getHadoopDistro());

		assertSame(containerOpts.getJmxPort(), opts.getJmxPort());
		assertSame(containerOpts.isJmxEnabled(), opts.isJmxEnabled());
		// Better to treat derived options as not explicit
		assertFalse(containerOpts.isExplicit(opts.getXDHomeDir()));
		assertFalse(containerOpts.isExplicit(opts.getTransport()));
		assertFalse(containerOpts.isExplicit(opts.getAnalytics()));
		assertFalse(containerOpts.isExplicit(opts.getStore()));
		assertFalse(containerOpts.isExplicit(opts.getHadoopDistro()));
		assertFalse(containerOpts.isExplicit(opts.getHttpPort()));
		assertFalse(containerOpts.isExplicit(opts.getJmxPort()));
		assertFalse(containerOpts.isExplicit(opts.isJmxEnabled()));

	}

	@Test
	public void testDefaultOptions() {
		SingleNodeOptions opts = new SingleNodeOptions();
		new CommandLineParser(opts).parseArgument(new String[] {});
		assertEquals("..", opts.getXDHomeDir());
		assertEquals(Transport.local, opts.getTransport());
		assertEquals(Analytics.memory, opts.getAnalytics());
		assertEquals(Store.memory, opts.getStore());
		assertEquals(HadoopDistro.hadoop10, opts.getHadoopDistro());
		assertEquals(9393, (int) opts.getHttpPort());
		assertEquals(8778, (int) opts.getJmxPort());
		assertEquals(false, opts.isJmxEnabled());

		assertFalse(opts.isExplicit(opts.getXDHomeDir()));
		assertFalse(opts.isExplicit(opts.getTransport()));
		assertFalse(opts.isExplicit(opts.getAnalytics()));
		assertFalse(opts.isExplicit(opts.getStore()));
		assertFalse(opts.isExplicit(opts.getHadoopDistro()));
		assertFalse(opts.isExplicit(opts.getHttpPort()));
		assertFalse(opts.isExplicit(opts.getJmxPort()));
		assertFalse(opts.isExplicit(opts.isJmxEnabled()));
	}

	@Test
	public void testJmxOptions() {
		SingleNodeOptions opts = new SingleNodeOptions();
		new CommandLineParser(opts).parseArgument(new String[] { "--jmxPort", "1234" });
		assertEquals(1234, (int) opts.getJmxPort());
		assertTrue(opts.isExplicit(opts.getJmxPort()));
		new CommandLineParser(opts).parseArgument(new String[] { "--jmxPort", "8778" });
		assertTrue(opts.isExplicit(opts.getJmxPort()));

	}
}
