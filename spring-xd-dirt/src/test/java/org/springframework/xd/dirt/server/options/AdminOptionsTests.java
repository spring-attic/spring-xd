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

import org.junit.After;
import org.junit.Test;

import org.springframework.xd.dirt.server.AdminMain;


/**
 * 
 * @author David Turanski
 */
public class AdminOptionsTests {

	@Test
	public void testAdminOptionsWithOptions() {
		AdminOptions opts = AdminMain.parseOptions(new String[] { "--httpPort", "0", "--transport", "local", "--store",
			"memory", "--analytics", "memory"});

		assertEquals(Store.memory, opts.getStore());
		assertEquals(Analytics.memory, opts.getAnalytics());
		assertEquals(false, opts.isJmxEnabled());

		assertEquals(0, opts.getHttpPort());
		assertEquals(8778, opts.getJmxPort());

	}

	@Test
	public void testDefaultOptions() {
		AdminOptions opts = AdminMain.parseOptions(new String[] {});
		assertEquals(Transport.redis, opts.getTransport());
		assertEquals(Analytics.redis, opts.getAnalytics());
		assertEquals(Store.redis, opts.getStore());
		assertEquals(HadoopDistro.hadoop10, opts.getHadoopDistro());
		assertEquals(8080, opts.getHttpPort());
		assertEquals(8778, opts.getJmxPort());
		assertEquals(false, opts.isJmxEnabled());
	}

	@Test
	public void testJmxOptions() {
		AdminOptions opts = AdminMain.parseOptions(new String[] { "--jmxPort", "1234" });
		assertEquals(1234, opts.getJmxPort());
	}

	@After
	public void cleanUp() {
	}
}
