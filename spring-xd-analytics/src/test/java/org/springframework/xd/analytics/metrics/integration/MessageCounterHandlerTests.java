package org.springframework.xd.analytics.metrics.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.analytics.metrics.core.CounterRepository;
import org.springframework.xd.analytics.metrics.redis.RedisCounterService;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MessageCounterHandlerTests {

	@Autowired
	private RedisCounterService counterService;
	
	@Autowired
	private CounterRepository repo;
	
	@Before
	@After
	public void initAndCleanup() {
		repo.delete("tupleCounter");
	}
	
	@Test
	public void messageCountTest() {
		Counter counter = counterService.getOrCreate("tupleCounter");
		
		//Counter counter = repo.findByName("tupleCounter");
		assertThat(counter.getValue(), equalTo(0L));
		MessageCounterHandler handler = new MessageCounterHandler(counterService, "tupleCounter");
		Message<String> message = MessageBuilder.withPayload("Hi").build();
		
		handler.process(message);
		counter = repo.findOne("tupleCounter");
		assertThat(counter.getValue(), equalTo(1L));
		
	}

}
