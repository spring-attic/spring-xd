package org.springframework.xd.analytics.metrics.redis;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 
 * @author Luke Taylor
 */
class RedisUtils {

	static <K, V> RedisTemplate<K, V> createRedisTemplate(RedisConnectionFactory connectionFactory, Class<V> valueClass) {
		RedisTemplate<K, V> redisTemplate = new RedisTemplate<K, V>();
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new GenericToStringSerializer<V>(valueClass));

		// avoids proxy
		redisTemplate.setExposeConnection(true);

		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.afterPropertiesSet();
		return redisTemplate;
	}
}
