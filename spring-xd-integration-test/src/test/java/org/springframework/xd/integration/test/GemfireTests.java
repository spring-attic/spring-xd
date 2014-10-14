/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.integration.test;

import static org.junit.Assert.*;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.ClientCache;
import com.gemstone.gemfire.cache.client.ClientCacheFactory;
import com.gemstone.gemfire.cache.client.ClientRegionShortcut;
import com.gemstone.gemfire.pdx.PdxInstance;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.xd.test.fixtures.GemfireServerSink;

/**
 * @author David Turanski
 */
public class GemfireTests extends AbstractIntegrationTest {
	//TODO should come from XDEnvironment
	private static String gemfireServerHost = "ec2-184-73-124-92.compute-1.amazonaws.com";
	private static int gemfireServerPort = 40404;

	private static ClientCache clientCache;
	private static Region<?,?> stocks;

	@BeforeClass
	static public void setUp() {
		 clientCache = new ClientCacheFactory().addPoolServer(gemfireServerHost,
				gemfireServerPort).create();
		 stocks = clientCache.createClientRegionFactory(ClientRegionShortcut.PROXY).create
				("Stocks");
		if (stocks.containsKey("gftest1")) {
			stocks.destroy("gftest1");
		}
		if (stocks.containsKey("gftest2")) {
			stocks.destroy("gftest2");
		}
	}
	@Test
	public void testBasicGemfireSink() throws InterruptedException {
		stream("gftest1", sources.http() + XD_DELIMITER + new GemfireServerSink("Stocks").host(gemfireServerHost)
				.port(gemfireServerPort));
		String data = "foo";
		sources.http(getContainerHostForSource("gftest1")).postData(data);
		Thread.sleep(1000);
		String cachedData = (String) stocks.get("gftest1");
		assertEquals(data,cachedData);
	}

	@Test
	public void testGemfireJsonSink() throws InterruptedException {
		stream("gftest2", sources.http() + XD_DELIMITER + new GemfireServerSink("Stocks").json(true).host
				(gemfireServerHost).port(gemfireServerPort));
		String data = "{\"foo\":\"foo\"}";
		sources.http(getContainerHostForSource("gftest2")).postData(data);
		Thread.sleep(1000);
		Object cachedData = stocks.get("gftest2");
		assertNotNull(cachedData);
		assertTrue(cachedData instanceof PdxInstance);
		assertEquals("foo",((PdxInstance)cachedData).getField("foo"));
	}
}
