package org.springframework.xd.analytics.metrics.redis;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

//Hack due to some resource constrains on CI machine...need to revisit.
class TestUtils {


	private static JedisConnectionFactory cf;
	private static StringRedisTemplate stringRedisTemplate;
	static {
		cf = new JedisConnectionFactory();
		cf.afterPropertiesSet();
		stringRedisTemplate = new StringRedisTemplate(cf);
	}

	static JedisConnectionFactory getJedisConnectionFactory() {
		return cf;
	}

	static StringRedisTemplate getStringRedisTemplate() {
		return stringRedisTemplate;
	}
}
