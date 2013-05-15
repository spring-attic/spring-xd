package org.springframework.xd.analytics.metrics.redis;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

//Hack due to some resource constrains on CI machine...need to revisit.
class TestUtils {


	private static LettuceConnectionFactory cf;
	private static StringRedisTemplate stringRedisTemplate;
	static {
		cf = new LettuceConnectionFactory();
		cf.setHostName("localhost");
		cf.setPort(6379);
		cf.afterPropertiesSet();
		stringRedisTemplate = new StringRedisTemplate(cf);
	}

	static RedisConnectionFactory getRedisConnectionFactory() {
		return cf;
	}

	static StringRedisTemplate getStringRedisTemplate() {
		return stringRedisTemplate;
	}
}
