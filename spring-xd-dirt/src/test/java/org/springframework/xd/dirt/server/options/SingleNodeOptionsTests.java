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

import org.junit.Test;
import org.springframework.xd.dirt.server.SingleNodeMain;

import static org.junit.Assert.*;


/**
 * 
 * @author David Turanski
 */
public class SingleNodeOptionsTests {

	@Test
	public void testSingleNodeOptionsWithOptions() {
		SingleNodeOptions opts = SingleNodeMain.parseOptions(new String[] { "--httpPort", "0", "--transport", "local",
			"--store",
			"memory", "--analytics", "memory", "--enableJmx", "true" });

		assertEquals(Store.memory, opts.getStore());
		assertEquals(Analytics.memory, opts.getAnalytics());
		assertEquals(true, opts.isJmxEnabled());

		assertEquals(0, opts.getHttpPort());
		assertEquals(8778, opts.getJmxPort());

	}

	@Test
	public void testDefaultOptions() {
		SingleNodeOptions opts = SingleNodeMain.parseOptions(new String[] {});
		assertEquals("..", opts.getXDHomeDir());
		assertEquals(Transport.local, opts.getTransport());
		assertEquals(Analytics.memory, opts.getAnalytics());
		assertEquals(Store.memory, opts.getStore());
		assertEquals(HadoopDistro.hadoop10, opts.getHadoopDistro());
		assertEquals(8080, opts.getHttpPort());
		assertEquals(8778, opts.getJmxPort());
		assertEquals(false, opts.isJmxEnabled());
	}

	@Test
	public void testJmxOptions() {
		SingleNodeOptions opts = SingleNodeMain.parseOptions(new String[] { "--jmxPort", "1234" });
		assertEquals(1234, opts.getJmxPort());
	}
}
