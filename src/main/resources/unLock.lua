-- 比较线程标示与锁中的标示是否一致
-- redis.call() 是 redis 中的调用脚本的命令，第一个参数为指令，第二个参数为 key 值， 第三个参数为 value 值
-- KEYS[1] 相当于 local key = KEYS[1]
-- ARGV[1] 相当于 local threadId = ARGV[1]
if (redis.call('get', KEYS[1]) == ARGV[1]) then
    -- 释放锁 del key
    return redis.call('del', KEYS[1])
end
return 0