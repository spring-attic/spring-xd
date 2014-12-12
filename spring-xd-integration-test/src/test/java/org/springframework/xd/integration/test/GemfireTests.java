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
import com.gemstone.gemfire.pdx.JSONFormatter;
import com.gemstone.gemfire.pdx.PdxInstance;
import org.junit.Before;
import org.junit.Test;

/**
 * @author David Turanski
 */
public class GemfireTests extends AbstractIntegrationTest {


	private ClientCache clientCache;

	private Region<Object, Object> stocks;

	private String streamName = "gftest";

	@Before
	public void setUp() {
		clientCache =
				new ClientCacheFactory().addPoolServer(xdEnvironment.getGemfireHost(), xdEnvironment.getGemfirePort())
						.create();
		stocks = clientCache.getRegion("/Stocks");
		if (stocks == null) {
			stocks = clientCache.createClientRegionFactory(ClientRegionShortcut.PROXY).create("Stocks");
		}
		assertNotNull("client region not created.", stocks);
		stocks.clear();

	}

	@Test
	public void testBasicGemfireSink()  {
		stream(streamName, sources.http() + XD_DELIMITER + sinks.gemfireServer("Stocks"));
		String data = "foo";
		sources.httpSource(streamName).postData(data);
		waitForCacheUpdate(streamName);
		String cachedData = (String) stocks.get(streamName);
		assertEquals(data, cachedData);
	}

	@Test
	public void testGemfireJsonSink()  {
		stream(streamName, sources.http() + XD_DELIMITER + sinks.gemfireServer("Stocks").json(true));
		String data = "{\"foo\":\"foo\"}";
		sources.httpSource(streamName).postData(data);
		waitForCacheUpdate(streamName);
		Object cachedData = stocks.get(streamName);

		assertNotNull(cachedData);
		assertTrue(cachedData instanceof PdxInstance);
		assertEquals("foo", ((PdxInstance) cachedData).getField("foo"));
	}

	@Test
	public void testGemfireSourceWithJsonObject()  {
		stream(sources.gemFireSource("Stocks") + XD_DELIMITER + sinks.file());

		String data = "{\"symbol\":\"FAKE\",\"price\":73}";

		stocks.put("FAKE", JSONFormatter.fromJSON(data));
		waitForCacheUpdate("FAKE");

		assertValid(data, sinks.file());
	}

	@Test
	public void testGemfireSourceWithString()  {
		stream(sources.gemFireSource("Stocks") + XD_DELIMITER + sinks.file());

		String data = "{\"symbol\":\"FAKE\",\"price\":73}";

		stocks.put("FAKE", data);
		waitForCacheUpdate("FAKE");

		assertValid(data, sinks.file());
	}

	@Test
	public void testGemfireCQSourceWithJsonObject()  {
		String query = "'Select * from /Stocks where symbol=''FAKE'''";
		stream(sources.gemFireSource("Stocks") + XD_DELIMITER + sinks.file());

		String data = "{\"symbol\":\"FAKE\",\"price\":73}";

		stocks.put("FAKE", JSONFormatter.fromJSON(data));
		waitForCacheUpdate("FAKE");
		assertValid(data, sinks.file());
	}


	private void waitForCacheUpdate(Object key)  {
		final long timeout = 5000;
		long waittime = 0;
		try {
			while (stocks.get(key) == null && waittime < timeout) {
				Thread.sleep(100);
				waittime += 100;
			}
		}catch(InterruptedException ie){
			throw new IllegalStateException(ie);
		}
	}
}
