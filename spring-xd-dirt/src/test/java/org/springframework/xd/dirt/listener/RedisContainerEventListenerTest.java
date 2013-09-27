/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.listener;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.dirt.container.ContainerStartedEvent;
import org.springframework.xd.dirt.container.ContainerStoppedEvent;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.test.redis.RedisTestSupport;

/**
 * Integration test of {@link RedisContainerEventListener}
 * 
 * @author Jennifer Hickey
 * @author Gary Russell
 * 
 */
@ContextConfiguration(classes = RedisContainerEventListenerTestConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisContainerEventListenerTest {

	@Rule
	public RedisTestSupport redisAvailableRule = new RedisTestSupport();

	@Autowired
	private ApplicationContext context;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Mock
	private XDContainer container;

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
		when(container.getJvmName()).thenReturn("123@test");
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


@Configuration
@ImportResource("org/springframework/xd/dirt/listener/RedisContainerEventListenerTest-context.xml")
class RedisContainerEventListenerTestConfig {

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		try {
			LettuceConnectionFactory cf = new LettuceConnectionFactory();
			cf.setHostName("localhost");
			cf.setPort(6379);
			cf.afterPropertiesSet();
			return cf;
		}
		catch (RedisConnectionFailureException e) {
			RedisConnectionFactory mockCF = mock(RedisConnectionFactory.class);
			when(mockCF.getConnection()).thenReturn(mock(RedisConnection.class));
			return mockCF;
		}
	}
}
