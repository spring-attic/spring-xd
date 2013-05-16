package org.springframework.xd.analytics.metrics;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;


import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.xd.analytics.metrics.core.FieldValueCounter;
import org.springframework.xd.analytics.metrics.core.FieldValueCounterRepository;
import org.springframework.xd.analytics.metrics.core.FieldValueCounterService;

/** 
 * @author Mark Pollack
 */
public abstract class AbstractRedisFieldValueCounterServiceTests {

	@Autowired
	protected FieldValueCounterService fieldValueCounterService;
	
	@Autowired
	protected FieldValueCounterRepository fieldValueCounterRepository;
	
	private final String tickersFieldValueCounterName = "tickersFieldValueCounter";
	
	private final String mentionsFieldValueCounterName = "mentionsFieldValueCounter";
	
	@Test
	public void testCrud() {
		FieldValueCounterRepository repo = fieldValueCounterRepository;
		FieldValueCounter fvTickersCounter = fieldValueCounterService.getOrCreate(tickersFieldValueCounterName);		
		//TODO might change behavior to have an empty FieldValueCounter vs. null after 'getOrCreate'
		assertThat(repo.findOne(fvTickersCounter.getName()), notNullValue());
		
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
		assertThat(repo.findOne(fvMentionsCounter.getName()), notNullValue());
		
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
