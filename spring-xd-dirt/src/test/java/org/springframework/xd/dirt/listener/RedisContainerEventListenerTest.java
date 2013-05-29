package org.springframework.xd.dirt.listener;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.dirt.core.Container;
import org.springframework.xd.dirt.event.ContainerStartedEvent;
import org.springframework.xd.dirt.event.ContainerStoppedEvent;

/**
 * Integration test of {@link RedisContainerEventListener}
 * 
 * @author Jennifer Hickey
 * 
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisContainerEventListenerTest {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Mock
	private Container container;

	private final String containerId = "test" + UUID.randomUUID().toString();

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@After
	public void tearDown() {
		redisTemplate.boundHashOps("containers").delete(containerId);
	}

	@Test
	public void testContainerStarted() {
		when(container.getId()).thenReturn(containerId);
		context.publishEvent(new ContainerStartedEvent(container));
		assertNotNull(redisTemplate.boundHashOps("containers").get(containerId));
	}

	@Test
	public void testContainerStopped() {
		when(container.getId()).thenReturn(containerId);
		redisTemplate.boundHashOps("containers").put(containerId, "container1");
		context.publishEvent(new ContainerStoppedEvent(container));
		assertNull(redisTemplate.boundHashOps("containers").get(containerId));
	}
}
