package org.springframework.xd.analytics.metrics.redis;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.xd.analytics.metrics.SharedFieldValueCounterRepositoryTests;
import org.springframework.xd.analytics.metrics.common.ServicesConfig;

@ContextConfiguration(classes=ServicesConfig.class, loader=AnnotationConfigContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisFieldValueCounterRepositoryTests extends SharedFieldValueCounterRepositoryTests {

	@After
	@Before
	public void beforeAndAfter() {
		fvRepository.deleteAll();
	}
	

}
