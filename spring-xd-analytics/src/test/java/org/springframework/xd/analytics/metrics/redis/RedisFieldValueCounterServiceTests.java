package org.springframework.xd.analytics.metrics.redis;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.xd.analytics.metrics.AbstractRedisFieldValueCounterServiceTests;
import org.springframework.xd.analytics.metrics.common.ServicesConfig;


@ContextConfiguration(classes=ServicesConfig.class, loader=AnnotationConfigContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisFieldValueCounterServiceTests extends AbstractRedisFieldValueCounterServiceTests {

	@After
	@Before
	public void beforeAndAfter() {
		fieldValueCounterRepository.deleteAll();
	}	
	
}
