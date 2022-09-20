package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @BelongProject: hm-dianping
 * @BelongPackage: com.hmdp.utils
 * @Author: 那个小楠瓜
 * @CreateTime: 2022-09-19 21:24
 * @Description: redis 数据
 * @Version: 1.0
 */
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
