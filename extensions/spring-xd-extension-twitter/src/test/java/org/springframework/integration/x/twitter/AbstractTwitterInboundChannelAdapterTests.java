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

package org.springframework.integration.x.twitter;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.social.twitter.api.impl.TwitterTemplate;
import org.springframework.web.client.RestTemplate;


/**
 *
 * @author Gary Russell
 */
public class AbstractTwitterInboundChannelAdapterTests {

	@Test
	public void testStop() throws Exception {
		TwitterTemplate twitter = mock(TwitterTemplate.class);
		RestTemplate template = mock(RestTemplate.class);
		when(twitter.getRestTemplate()).thenReturn(template);
		final CountDownLatch latch = new CountDownLatch(1);
		AbstractTwitterInboundChannelAdapter adapter = new AbstractTwitterInboundChannelAdapter(twitter) {

			@Override
			protected void doSendLine(String line) {
			}

			@Override
			protected URI buildUri() {
				latch.countDown();
				return null;
			}

		};
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();
		adapter.start();
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		adapter.stop();
		ExecutorService taskExecutor = TestUtils.getPropertyValue(adapter, "taskExecutor.threadPoolExecutor",
				ExecutorService.class);
		assertTrue(taskExecutor.isShutdown());
	}

}
