package org.springframework.xd.analytics.metrics.redis;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.xd.analytics.metrics.AbstractRichGaugeServiceTests;
import org.springframework.xd.analytics.metrics.common.ServicesConfig;
import org.springframework.xd.analytics.metrics.core.RichGaugeService;

/**
 * @author Luke Taylor
 */
@ContextConfiguration(classes=ServicesConfig.class, loader=AnnotationConfigContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisRichGaugeServiceTests extends AbstractRichGaugeServiceTests {

	@Autowired
	private RedisRichGaugeRepository repo;
	
	@Autowired
	private RedisRichGaugeService service;

	@After
	@Before
	public void beforeAndAfter() {
		repo.deleteAll();
	}

	@Override
	protected RichGaugeService createService() {
		return service;
	}
}
