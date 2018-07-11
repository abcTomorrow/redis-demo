package com.wojiushiwo;

import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.UUID;

/**
 * 分布式锁四个特性：
 * 1、互斥性 在任何时刻 只有一个客户端持有锁
 * 2、不会发生死锁 即使有一个客户端在持有锁的期间崩溃而没有主动解锁 也能保证后续其他客户端能加锁
 * 3、具有容错性 只要大部分Redis节点正常运行 客户端就可以加锁和解锁
 * 4、加锁和解锁必须是同一个客户端，客户端自己不能把别人加的锁解了
 */
public class RedisLock {
    public String getLock1(String key, int timeOut) {
        try {
            Jedis jedis = RedisManager.getJedis();
            String value = UUID.randomUUID().toString();
            long endTime = System.currentTimeMillis() + timeOut;
            while (System.currentTimeMillis() < endTime) { //设置一个轮询
                //setnx key不存在 1 key已经存在0
                if (jedis.setnx(key, value) == 1) {
                    //设置key过期时间
                    jedis.expire(key, timeOut);
                    //在有效期内 可以返回value
                    return value;
                }
                //如果程序在setnx时出问题 那么就无法设置过期时间 key会一直被锁
                if (jedis.ttl(key) == -1) {//未设置过期时间
                    jedis.expire(key, timeOut);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //超过了过期时间 key会被del 所以会返回null
        return null;
    }

    public String getLock2(String key, int timeOut) {
        try {
            Jedis jedis = RedisManager.getJedis();
            String value = UUID.randomUUID().toString();
            String result = jedis.set(key, value, "NX", "PX", timeOut);
            if ("OK".equals(result)) {
                return value;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //不对的做法 不能保证原子操作
    //解锁 在锁未过期之前
    //如果锁已经过期 会自动del 不需要解锁了
    public void releaseLock1(String key, String value) {
        try {
            Jedis jedis = RedisManager.getJedis();
            String result = jedis.get(key);
            //无法保证原子操作 如果key在解锁前已经过期了 而别的客户端再次为key加锁 这时del会把别的客户端的锁解了
            if (value.equals(result)) {
                jedis.del(key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //lua语言 能保证原子操作 如果拿到key的值=value 则删除 释放锁
    public boolean releaseLock2(String key, String value) {
        try {
            Jedis jedis = RedisManager.getJedis();
            String script = "if redis.call('GET',KEYS[1])==ARGV[1]) then return redis.call('DEL',KEYS[1]) else 0 end";
            Object eval = jedis.eval(script, Collections.singletonList(key), Collections.singletonList(value));
            if ("OK".equals(eval)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
