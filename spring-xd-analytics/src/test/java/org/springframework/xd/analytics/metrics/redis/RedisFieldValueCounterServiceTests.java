package org.springframework.xd.analytics.metrics.redis;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.analytics.metrics.core.FieldValueCounter;


@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisFieldValueCounterServiceTests {

	@Autowired
	private RedisFieldValueCounterService fieldValueCounterService;
		
	@Autowired
	private RedisFieldValueCounterRepository repo;
	
	private final String tickersFieldValueCounterName = "tickersFieldValueCounter";
	
	private final String mentionsFieldValueCounterName = "mentionsFieldValueCounter";
	
	@Before
	@After
	public void initAndCleanup() {
		repo.delete(tickersFieldValueCounterName);
		repo.delete(mentionsFieldValueCounterName);
	}
	
	@Test
	public void testCrud() {

		FieldValueCounter fvTickersCounter = fieldValueCounterService.getOrCreate(tickersFieldValueCounterName);		
		//TODO might change behavior to have an empty FieldValueCounter vs. null after 'getOrCreate'
		assertThat(repo.findOne(fvTickersCounter.getName()), is(nullValue()));
		
		fieldValueCounterService.increment(tickersFieldValueCounterName, "VMW");
		Map<String, Double> counts = repo.findOne(tickersFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("VMW"), equalTo(1.0));
		
		fieldValueCounterService.increment(tickersFieldValueCounterName, "VMW");
		counts = repo.findOne(tickersFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("VMW"), equalTo(2.0));
		
		fieldValueCounterService.increment(tickersFieldValueCounterName, "ORCL");
		counts = repo.findOne(tickersFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("ORCL"), equalTo(1.0));
		
		FieldValueCounter fvMentionsCounter = fieldValueCounterService.getOrCreate(mentionsFieldValueCounterName);		
		//TODO might change behavior to have an empty FieldValueCounter vs. null after 'getOrCreate'
		assertThat(repo.findOne(fvMentionsCounter.getName()), is(nullValue()));
		
		fieldValueCounterService.increment(mentionsFieldValueCounterName, "mama");
		counts = repo.findOne(mentionsFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("mama"), equalTo(1.0));
		
		fieldValueCounterService.increment(mentionsFieldValueCounterName, "mama");
		counts = repo.findOne(mentionsFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("mama"), equalTo(2.0));
		
		fieldValueCounterService.increment(mentionsFieldValueCounterName, "papa");
		counts = repo.findOne(mentionsFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("papa"), equalTo(1.0));
		
		fieldValueCounterService.decrement(tickersFieldValueCounterName, "VMW");
		counts = repo.findOne(tickersFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("VMW"), equalTo(1.0));
		
		fieldValueCounterService.decrement(mentionsFieldValueCounterName, "mama");
		counts = repo.findOne(mentionsFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("mama"), equalTo(1.0));
		
	}
	
}
