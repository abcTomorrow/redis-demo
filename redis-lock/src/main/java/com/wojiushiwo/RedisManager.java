package com.wojiushiwo;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisManager {
    private static JedisPool jedisPool;
    private static final String host = "127.0.0.1";
    private static final int port = 6379;

    static {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(10);
        jedisPoolConfig.setMaxTotal(20);
        jedisPool = new JedisPool(jedisPoolConfig, host, port);
    }

    public static Jedis getJedis() throws Exception {
        if (jedisPool != null) {
            return jedisPool.getResource();
        }
        throw new Exception("jedisPool is null");
    }
}
