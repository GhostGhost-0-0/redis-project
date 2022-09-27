package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @BelongProject: hm-dianping
 * @BelongPackage: com.hmdp.config
 * @Author: 那个小楠瓜
 * @CreateTime: 2022-09-23 19:35
 * @Description: redisson 配置类
 * @Version: 1.0
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedissonClient redissonClient() {
        // 配置类
        Config config = new Config();
        // 添加 redis 地址，这里添加了单点的地址，也可以使用 config.useClusterServers() 添加集群地址
        config.useSingleServer().setAddress("redis://127.0.0.1:6001");
        // 创建客户端
        return Redisson.create(config);
    }

}
