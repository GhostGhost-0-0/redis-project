package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @BelongProject: hm-dianping
 * @BelongPackage: com.hmdp.utils
 * @Author: 那个小楠瓜
 * @CreateTime: 2022-09-23 15:37
 * @Description: 基于 redis 实现的初级版分布式锁
 * @Version: 1.0
 */

public class SimpleRedisLock implements ILok{

    /**
     * 锁的名称
     */
    private String name;
    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     *  redis key 前缀
     */
    private static final String KEY_PREFIX = "lock:";

    /**
     * 线程标识前缀
     */
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取当前线程标示
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        // 不直接返回 success 是因为涉及自动装箱有可能出现空指针异常
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        // 获取线程标示
        String thredId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁中的线程标示
        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        // 判断标示是否一致
        if (thredId.equals(id)) {
            // 释放锁
            stringRedisTemplate.delete(KEY_PREFIX + name);
        }
    }
}
