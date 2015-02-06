/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.xd.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPoolConfig;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for Redis Sink
 * @author Mark Pollack
 */
@Configuration
public class RedisSinkConfiguration {

    @Value("${hostname}")
    private String hostname;

    @Value("${port}")
    private int port;

    @Value("${password}")
    private String password;

    @Value("${database}")
    private int database = 0;

    @Value("${sentinelMaster}")
    private String sentinelMaster;

    @Value("${sentinelNodes}")
    private String sentinelNodes;

    @Value("${maxIdle}")
    private int maxIdle = 8;

    @Value("${minIdle}")
    private int minIdle = 0;

    @Value("${maxActive}")
    private int maxActive = 8;

    @Value("${maxWait}")
    private int maxWait = -1;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() throws UnknownHostException {
        return applyProperties(createJedisConnectionFactory());
    }

    protected final JedisConnectionFactory applyProperties(JedisConnectionFactory factory) {
        factory.setHostName(hostname);
        factory.setPort(port);
        if (StringUtils.hasText(password)) {
            factory.setPassword(password);
        }
        factory.setDatabase(database);
        return factory;
    }

    protected final RedisSentinelConfiguration getSentinelConfig() {
        if (StringUtils.hasText(sentinelNodes)) {
            RedisSentinelConfiguration config = new RedisSentinelConfiguration();
            config.master(sentinelMaster);
            config.setSentinels(createSentinels());
            return config;
        }
        return null;
    }

    private JedisConnectionFactory createJedisConnectionFactory() {
        return new JedisConnectionFactory(getSentinelConfig(), jedisPoolConfig());
    }

    private List<RedisNode> createSentinels() {
        List<RedisNode> sentinels = new ArrayList<RedisNode>();
        for (String node : StringUtils.commaDelimitedListToStringArray(sentinelNodes)) {
            try {
                String[] parts = StringUtils.split(node, ":");
                Assert.state(parts.length == 2, "Must be defined as 'host:port'");
                sentinels.add(new RedisNode(parts[0], Integer.valueOf(parts[1])));
            }
            catch (RuntimeException ex) {
                throw new IllegalStateException("Invalid redis sentinel "
                        + "property '" + node + "'", ex);
            }
        }
        return sentinels;
    }

    private JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxActive);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setMaxWaitMillis(maxWait);
        return config;
    }

}
