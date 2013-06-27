package org.springframework.xd.analytics.metrics.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.xd.analytics.metrics.redis.*;

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
	public RedisRichGaugeService redisRichGaugeService() {
		return new RedisRichGaugeService(redisRichGaugeRepository());
	}
	
	@Bean
	public RedisRichGaugeRepository redisRichGaugeRepository() {
		return new RedisRichGaugeRepository(redisConnectionFactory());
	}
	
	@Bean
	public RedisGaugeService redisGaugeService() {
		return new RedisGaugeService(redisGaugeRepository());
	}
	
	@Bean
	public RedisGaugeRepository redisGaugeRepository() {
		return new RedisGaugeRepository(redisConnectionFactory());
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
	public RedisAggregateCounterService redisAggregateCounterAggregate() {
		return new RedisAggregateCounterService(redisConnectionFactory());
	}

	@Bean
	public StringRedisTemplate stringRedisTemplate() {
		return new StringRedisTemplate(redisConnectionFactory());
	}
	
	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		LettuceConnectionFactory cf = new LettuceConnectionFactory();
		cf.setHostName("localhost");
		cf.setPort(6379);
		return cf;
	}
	
	
}
