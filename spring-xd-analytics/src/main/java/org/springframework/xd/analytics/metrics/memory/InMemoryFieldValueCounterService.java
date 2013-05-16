package org.springframework.xd.analytics.metrics.memory;

import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.MetricsException;
import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.analytics.metrics.core.FieldValueCounter;
import org.springframework.xd.analytics.metrics.core.FieldValueCounterRepository;
import org.springframework.xd.analytics.metrics.core.FieldValueCounterService;

public class InMemoryFieldValueCounterService implements
		FieldValueCounterService {

	private FieldValueCounterRepository fieldValueRepository;
	private final Object monitor = new Object();
	
	public InMemoryFieldValueCounterService(InMemoryFieldValueCounterRepository fieldValueRepository) {
		Assert.notNull(fieldValueRepository, "FieldValueCounterRepository can not be null");
		this.fieldValueRepository = fieldValueRepository;
	}
	
	
	@Override
	public FieldValueCounter getOrCreate(String name) {
		Assert.notNull(name, "FieldValueCounter name can not be null");
		synchronized (this.monitor){
			FieldValueCounter counter = fieldValueRepository.findOne(name);
			if (counter == null) {
				counter = new FieldValueCounter(name);
				this.fieldValueRepository.save(counter);
			}
			return counter;
		}
	}

	@Override
	public void increment(String name, String fieldName) {
		synchronized (monitor) {
			modifyFieldValue(name, fieldName, 1);
		}
	}

	@Override
	public void decrement(String name, String fieldName) {
		synchronized (monitor) {
			modifyFieldValue(name, fieldName, -1);
		}
	}

	@Override
	public void reset(String name, String fieldName) {
		FieldValueCounter counter = findCounter(name);
		Map<String, Double> data = counter.getFieldValueCount();
		if (data.containsKey(fieldName)) {
			data.put(fieldName, 0D);
		}
	}
	
	private FieldValueCounter findCounter(String name) {
		FieldValueCounter counter = this.fieldValueRepository.findOne(name);
		if (counter == null) {
			throw new MetricsException("FieldValueCounter " + name + " not found");
		}
		return counter;
	}
	
	private void modifyFieldValue(String name, String fieldName, double delta) {
		FieldValueCounter counter = findCounter(name);
		Map<String, Double> data = counter.getFieldValueCount();
		double count = data.containsKey(fieldName) ? data.get(fieldName) : 0;
		data.put(fieldName, count + delta);			
		this.fieldValueRepository.save(counter);
	}

}
