package com.bossien.flowtreatmentservice.utils;

import org.springframework.data.redis.connection.RedisConnection;


public class RedisConnectionUtils {

    public static Ops boundZSetOps(RedisConnection redisConnection, String key) {
        Ops ops = new Ops(key);
        ops.addRedisConnection(redisConnection);
        return ops;

    }

   public static class Ops {

        private String key;
        private RedisConnection redisConnection;

        public Ops(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        private void addRedisConnection(RedisConnection redisConnection) {
            this.redisConnection = redisConnection;
        }

        public void add(String value, Double score) {
            redisConnection.zAdd(key.getBytes(), score, value.getBytes());
        }

    }
}
