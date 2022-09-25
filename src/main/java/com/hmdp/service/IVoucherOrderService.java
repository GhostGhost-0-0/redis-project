package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @BelongProject: hm-dianping
 * @BelongPackage: com.hmdp.service
 * @Author: 那个小楠瓜
 * @CreateTime: 2022-09-19 21:15
 * @Description: 优惠券的订单表服务类
 * @Version: 1.0
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

    void createVoucherOrder(VoucherOrder voucherOrder);
}
