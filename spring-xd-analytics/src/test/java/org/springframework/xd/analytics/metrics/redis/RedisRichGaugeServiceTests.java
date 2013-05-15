package org.springframework.xd.analytics.metrics.redis;

import org.junit.After;
import org.junit.Before;
import org.springframework.xd.analytics.metrics.AbstractRichGaugeServiceTests;
import org.springframework.xd.analytics.metrics.core.RichGaugeService;

/**
 * @author Luke Taylor
 */
public class RedisRichGaugeServiceTests extends AbstractRichGaugeServiceTests {

	private RedisRichGaugeRepository repo;

	@After
	@Before
	public void beforeAndAfter() {
		repo = new RedisRichGaugeRepository(TestUtils.getRedisConnectionFactory());
		repo.deleteAll();
	}

	@Override
	protected RichGaugeService createService() {
		return new RedisRichGaugeService(new RedisRichGaugeRepository(TestUtils.getRedisConnectionFactory()));
	}
}
