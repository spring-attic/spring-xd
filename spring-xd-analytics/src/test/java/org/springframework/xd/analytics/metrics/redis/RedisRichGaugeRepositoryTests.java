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

package org.springframework.xd.analytics.metrics.redis;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.xd.analytics.metrics.AbstractRichGaugeRepositoryTests;
import org.springframework.xd.analytics.metrics.common.RedisRepositoriesConfig;
import org.springframework.xd.analytics.metrics.core.RichGauge;
import org.springframework.xd.analytics.metrics.core.RichGaugeRepository;
import org.springframework.xd.test.redis.RedisTestSupport;

/**
 * @author Luke Taylor
 * @author Gary Russell
 */
@ContextConfiguration(classes = RedisRepositoriesConfig.class, loader = AnnotationConfigContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisRichGaugeRepositoryTests extends AbstractRichGaugeRepositoryTests {

	@Rule
	public RedisTestSupport redisAvailableRule = new RedisTestSupport();

	@Autowired
	private RedisRichGaugeRepository repo;

	@Autowired
	private RedisConnectionFactory connectionFactory;


	@After
	@Before
	public void beforeAndAfter() {
		repo.deleteAll();
	}

	@Test
	public void testAtomicity() throws Exception {
		RedisRichGaugeRepository modified = new RedisRichGaugeRepository(connectionFactory) {
			/*
			 * Use a modified version of the repo that introduces a pause in the atomic unit, so that we can
			 * interfere from another thread.
			 */
			@Override
			public RichGauge findOne(String name) {
				try {
					Thread.sleep(500);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				return super.findOne(name);
			}


		};

		Thread concurrent = new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(200);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				repo.setValue("foo", 42.0D);
			}
		};


		concurrent.start();
		modified.setValue("foo", 1.0D);
		concurrent.join();

		RichGauge result = repo.findOne("foo");
		assertThat(result.getAverage(), equalTo(21.5D));

	}




	@Override
	protected RichGaugeRepository createService() {
		return repo;
	}
}
