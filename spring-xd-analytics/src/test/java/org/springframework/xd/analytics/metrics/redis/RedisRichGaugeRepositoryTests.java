
package org.springframework.xd.analytics.metrics.redis;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.xd.analytics.metrics.AbstractRichGaugeRepositoryTests;
import org.springframework.xd.analytics.metrics.common.RedisRepositoriesConfig;
import org.springframework.xd.analytics.metrics.core.RichGaugeRepository;
import org.springframework.xd.test.redis.RedisAvailableRule;

/**
 * @author Luke Taylor
 * @author Gary Russell
 */
@ContextConfiguration(classes = RedisRepositoriesConfig.class, loader = AnnotationConfigContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisRichGaugeRepositoryTests extends AbstractRichGaugeRepositoryTests {

	@Rule
	public RedisAvailableRule redisAvailableRule = new RedisAvailableRule();

	@Autowired
	private RedisRichGaugeRepository repo;

	@After
	@Before
	public void beforeAndAfter() {
		repo.deleteAll();
	}

	@Override
	protected RichGaugeRepository createService() {
		return repo;
	}
}
