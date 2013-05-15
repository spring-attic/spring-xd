package org.springframework.xd.analytics.metrics.redis;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import redis.clients.jedis.JedisPoolConfig;

//Hack due to some resource constrains on CI machine...need to revisit.
class TestUtils {


	private static JedisConnectionFactory cf;
	private static StringRedisTemplate stringRedisTemplate;
	static {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxActive(100);
		cf = new JedisConnectionFactory(poolConfig);
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
