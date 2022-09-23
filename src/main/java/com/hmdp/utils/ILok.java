package com.hmdp.utils;

/**
 * @BelongProject: hm-dianping
 * @BelongPackage: com.hmdp.utils
 * @Author: 那个小楠瓜
 * @CreateTime: 2022-09-23 15:33
 * @Description: 分布式锁
 * @Version: 1.0
 */
public interface ILok {

    /**
     * 尝试获取锁
     * @param timeoutSec 锁持有的超时时间，过期后自动释放
     * @return true 代表获取锁成功； false 代表获取锁失败
     */
    boolean tryLock(long timeoutSec);

    void unlock();
}
