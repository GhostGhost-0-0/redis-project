package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

/**
 * @BelongProject: hm-dianping
 * @BelongPackage: com.hmdp.utils
 * @Author: 那个小楠瓜
 * @CreateTime: 2022-09-21 14:23
 * @Description: TODO
 * @Version: 1.0
 */
@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(1);

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 存入 redis
     * @param key key 值
     * @param value 要存入的数据
     * @param time 过期时间
     * @param unit 时间类型如秒，分
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 存入 redis（带逻辑过期时间）
     * @param key key 值
     * @param value 要存入的数据
     * @param time 过期时间
     * @param unit 时间类型如：秒，分
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        // 设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入 redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 使用存入空值方案解决缓存穿透
     * @param keyPrefix redis key 前缀
     * @param id id
     * @param type 具体类型
     * @param dbFallback 函数，要求传入一段跟操作数据库的函数，例如 根据 id 查询数据库
     * @param time 过期时间
     * @param unit 时间类型如：秒，分
     * @return 从缓存中查到的数据
     * @param <R> 与 type 同一类型的数据
     * @param <ID> id 的数据类型，如Long、String 等
     */
    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        // 1.从 redis 查询缓存
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(json)) {
            // 3.存在，直接返回
            return JSONUtil.toBean(json, type);
        }
        // 判断是否命中的是空值
        if (json != null) {
            // 返回一个错误信息
            return null;
        }
        // 4.不存在，根据 id 查询数据库
        R r = dbFallback.apply(id);
        // 5.不存在，返回错误
        if (r == null) {
            // 把空值写入 redis
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 6.存在，写入 redis
        this.set(key, r, time, unit);
        // 7.返回
        return r;
    }

    /**
     * 使用互斥锁方案解决缓存击穿
     * @param keyPrefix redis key 前缀
     * @param id id
     * @param type 具体类型
     * @param dbFallback 函数，要求传入一段跟操作数据库的函数，例如 根据 id 查询数据库
     * @param time 过期时间
     * @param unit 时间类型如：秒，分
     * @return 从缓存中查到的数据
     * @param <R> 与 type 同一类型的数据
     * @param <ID> id 的数据类型，如Long、String 等
     */
    public <R, ID> R queryWithMutex(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        // 1.从 redis 查询缓存
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(json)) {
            // 3.存在，直接返回
            return JSONUtil.toBean(json, type);
        }
        // 判断是否命中的是空值
        if (json != null) {
            // 返回一个错误信息
            return null;
        }
        // 4.实现缓存重建
        // 4.1.获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        R r = null;
        try {
            boolean isLock = tryLock(lockKey);
            // 4.2.判断是否获取成功
            if (!isLock) {
                // 4.3.失败，则休眠并重试
                Thread.sleep(50);
                return queryWithMutex(keyPrefix, id, type, dbFallback, time, unit);
            }
            // 4.4.成功，根据 id 查询数据库
            r = dbFallback.apply(id);
            // 5.不存在，返回错误
            if (r == null) {
                // 把空值写入 redis
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            // 6.存在，写入 redis
            this.set(key, r, time, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 7.释放互斥锁
            unLock(lockKey);
        }
        // 7.返回
        return r;
    }

    /**
     * 使用逻辑过期方案解决缓存击穿
     * @param keyPrefix redis key 前缀
     * @param id id
     * @param type 具体类型
     * @param dbFallback 函数，要求传入一段跟操作数据库的函数，例如 根据 id 查询数据库
     * @param time 过期时间
     * @param unit 时间类型如：秒，分
     * @return 从缓存中查到的数据
     * @param <R> 与 type 同一类型的数据
     * @param <ID> id 的数据类型，如Long、String 等
     */
    public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        // 1.从 redis 查询缓存
        String key = keyPrefix + id;
        String Json = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isBlank(Json)) {
            // 3.不存在，直接返回 null
            return null;
        }
        // 4.命中，需要把 json 反序列化为对象
        RedisData redisData = JSONUtil.toBean(Json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 5.判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 5.1.未过期，直接返回店铺信息
            return r;
        }
        // 5.2.过期，需要缓存重建
        // 6.缓存重建
        // 6.1.获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 6.2.判断是否获取锁成功
        if (isLock) {
            // 6.3.成功，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 查询数据库
                    R r1 = dbFallback.apply(id);
                    // 写入 redis
                    this.setWithLogicalExpire(key, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放锁
                    unLock(lockKey);
                }
            });
        }
        // 6.4.返回过期的商铺信息
        return r;
    }

    /**
     * 获取互斥锁
     * @param key redis key
     * @return true or false
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放互斥锁锁
     * @param key redis key
     */
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }
}
