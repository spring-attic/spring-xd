package org.springframework.xd.analytics.metrics.memory;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.xd.analytics.metrics.AbstractRedisFieldValueCounterServiceTests;
import org.springframework.xd.analytics.metrics.common.InMemoryServicesConfig;

@ContextConfiguration(classes=InMemoryServicesConfig.class, loader=AnnotationConfigContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class InMemoryFieldValueCounterServiceTests extends AbstractRedisFieldValueCounterServiceTests {

}
