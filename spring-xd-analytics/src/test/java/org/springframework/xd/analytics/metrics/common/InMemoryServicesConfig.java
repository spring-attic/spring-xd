package org.springframework.xd.analytics.metrics.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.xd.analytics.metrics.memory.InMemoryCounterRepository;
import org.springframework.xd.analytics.metrics.memory.InMemoryCounterService;
import org.springframework.xd.analytics.metrics.memory.InMemoryFieldValueCounterRepository;
import org.springframework.xd.analytics.metrics.memory.InMemoryFieldValueCounterService;

@Configuration
public class InMemoryServicesConfig {
	
	@Bean
	public InMemoryFieldValueCounterService inMemoryFieldValueCounterService() {
		return new InMemoryFieldValueCounterService(inMemoryFieldValueCounterRepository());
	}
	
	@Bean
	public InMemoryFieldValueCounterRepository inMemoryFieldValueCounterRepository() {
		return new InMemoryFieldValueCounterRepository();
	}
	
	@Bean
	public InMemoryCounterService inMemoryCounterService() {
		return new InMemoryCounterService(inMemoryCounterRepository());
	}
	
	@Bean
	public InMemoryCounterRepository inMemoryCounterRepository() {	
		return new InMemoryCounterRepository();
	}
	
	
	
}
