package org.elastos.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;

public class DCountUtil {

    private RedisTemplate<String, Object> redis;

    private String counter = UUID.randomUUID().toString();

    public DCountUtil(String name, Long start, RedisTemplate<String, Object> re) throws RuntimeException{
        if (null == re) {
            throw new RuntimeException("Err DCountUtil must has a valid redis template");
        }
        redis = re;

        if (StringUtils.isBlank(name)) {
            counter = name;
        }
        if (null != start) {
            redis.boundValueOps(counter).set(start);
        } else {
            redis.boundValueOps(counter).set(0);
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        redis.delete(counter);
    }

    public Long add(long inc) {
        return redis.boundValueOps(counter).increment(inc);
    }

    public Long inc() {
        return redis.boundValueOps(counter).increment(1);
    }

    public void set(Long num) {
        redis.boundValueOps(counter).set(num);
    }
}
