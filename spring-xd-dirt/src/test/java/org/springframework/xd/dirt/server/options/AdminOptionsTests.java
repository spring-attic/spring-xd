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
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.kohsuke.args4j.CmdLineException;


/**
 * 
 * @author David Turanski
 */
public class AdminOptionsTests {

	@Test
	public void testAdminOptionsWithOptions() throws CmdLineException {
		AdminOptions opts = new AdminOptions();
		new CommandLineParser(opts).parseArgument(new String[] { "--httpPort", "0", "--transport",
			"local",
			"--store",
			"memory", "--analytics", "memory" });


		assertEquals(Store.memory, opts.getStore());
		assertEquals(Analytics.memory, opts.getAnalytics());
		assertEquals(false, opts.isJmxEnabled());

		assertEquals(0, (int) opts.getHttpPort());
		assertEquals(8778, (int) opts.getJmxPort());


		assertTrue(opts.isExplicit(opts.getHttpPort()));
		assertFalse(opts.isExplicit(opts.getJmxPort()));

	}

	@Test
	public void testDefaultOptions() {
		AdminOptions opts = new AdminOptions();
		new CommandLineParser(opts).parseArgument(new String[] {});
		assertEquals(Transport.redis, opts.getTransport());
		assertEquals(Analytics.redis, opts.getAnalytics());
		assertEquals(Store.redis, opts.getStore());
		assertEquals(HadoopDistro.hadoop10, opts.getHadoopDistro());
		assertEquals(8080, (int) opts.getHttpPort());
		assertEquals(8778, (int) opts.getJmxPort());
		assertEquals(false, (boolean) opts.isJmxEnabled());

		assertFalse(opts.isExplicit(opts.getTransport()));
		assertFalse(opts.isExplicit(opts.getAnalytics()));
		assertFalse(opts.isExplicit(opts.getStore()));
		assertFalse(opts.isExplicit(opts.getHadoopDistro()));
		assertFalse(opts.isExplicit(opts.getHttpPort()));
		assertFalse(opts.isExplicit(opts.isJmxEnabled()));
	}

	@Test
	public void testJmxOptions() throws CmdLineException {
		AdminOptions opts = new AdminOptions();
		new CommandLineParser(opts).parseArgument(new String[] { "--jmxPort", "1234" });
		assertEquals(1234, (int) opts.getJmxPort());
	}

	@After
	public void cleanUp() {
	}
}
