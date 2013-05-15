package org.springframework.xd.analytics.metrics.redis;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.xd.analytics.metrics.SharedFieldValueCounterRepositoryTests;

public class RedisFieldValueCounterRepositoryTests extends SharedFieldValueCounterRepositoryTests {

	
	@AfterClass
	@BeforeClass
	public static void beforeAndAfter() {
		fvRepository = new RedisFieldValueCounterRepository(TestUtils.getRedisConnectionFactory());
		fvRepository.deleteAll();
	}
	

}
