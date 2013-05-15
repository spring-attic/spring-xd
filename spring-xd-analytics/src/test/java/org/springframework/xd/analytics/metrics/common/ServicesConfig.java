package org.springframework.xd.analytics.metrics.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.xd.analytics.metrics.redis.RedisCounterRepository;
import org.springframework.xd.analytics.metrics.redis.RedisCounterService;
import org.springframework.xd.analytics.metrics.redis.RedisFieldValueCounterRepository;
import org.springframework.xd.analytics.metrics.redis.RedisFieldValueCounterService;

@Configuration
public class ServicesConfig {
	
	@Bean
	public RedisFieldValueCounterService fieldValueCounterService() {
		return new RedisFieldValueCounterService(redisFieldValueCounterRepository());
	}
	
	@Bean
	public RedisFieldValueCounterRepository redisFieldValueCounterRepository() {
		return new RedisFieldValueCounterRepository(redisConnectionFactory());
	}
	
	@Bean
	public RedisCounterService redisCounterService() {
		return new RedisCounterService(redisCounterRepository());
	}
	
	@Bean
	public RedisCounterRepository redisCounterRepository() {	
		return new RedisCounterRepository(redisConnectionFactory());
	}
	
	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		LettuceConnectionFactory cf = new LettuceConnectionFactory();
		cf.setHostName("localhost");
		cf.setPort(6379);
		return cf;
	}
	
	
}
