package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @BelongProject: hm-dianping
 * @BelongPackage: com.hmdp.service
 * @Author: 那个小楠瓜
 * @CreateTime: 2022-09-19 21:15
 * @Description: 商户信息表服务类
 * @Version: 1.0
 */
public interface IShopService extends IService<Shop> {

    Result queryById(Long id);
}
