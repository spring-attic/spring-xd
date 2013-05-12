package org.springframework.xd.analytics.metrics.redis;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@SuppressWarnings("unchecked")
class RedisUtils {

	static RedisTemplate createRedisTemplate(
			RedisConnectionFactory connectionFactory, Class<?> valueClass) {
		RedisTemplate redisTemplate = new RedisTemplate();
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new GenericToStringSerializer(valueClass));

		// avoids proxy
		redisTemplate.setExposeConnection(true);

		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.afterPropertiesSet();
		return redisTemplate;
	}
}
