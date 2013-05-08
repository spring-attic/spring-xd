package org.springframework.xd.analytics.metrics.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.xd.analytics.metrics.redis.RedisCounterRepository;
import org.springframework.xd.analytics.metrics.redis.RedisCounterService;

@Configuration
public class ServicesConfig {
	

	
	@Bean
	public RedisCounterService redisCounterService() {
		return new RedisCounterService(redisCounterRepository());
	}
	
	@Bean
	public RedisCounterRepository redisCounterRepository() {	
		return new RedisCounterRepository(jedisConnectionFactory());
	}
	
	@Bean
	public JedisConnectionFactory jedisConnectionFactory() {
		return new JedisConnectionFactory();
	}
	
	
}
