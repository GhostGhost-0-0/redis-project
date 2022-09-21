package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * @BelongProject: hm-dianping
 * @BelongPackage: com.hmdp.service.impl
 * @Author: 那个小楠瓜
 * @CreateTime: 2022-09-19 21:15
 * @Description: 优惠券的订单表服务实现类
 * @Version: 1.0
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Override
    @Transactional
    public Result seckillVoucher(Long voucherId) {
        // 1.查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2.判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始！");
        }
        // 3.判断秒杀是否已经结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束！");
        }
        // 4.判断库存是否充足
        if (voucher.getStock() < 1) {
            // 库存不足
            return Result.fail("库存不足！");
        }
        // 5.扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1") // set stock = stock - 1
                // 使用乐观锁解决超卖线程安全问题
                .eq("voucher_id", voucherId).gt("stock", 0) // where voucher_id = ? and stock > 0
                .update();
        if (!success) {
            // 扣减失败
            return Result.fail("库存不足！");
        }

        // 6.创建订单
        Long userId = UserHolder.getUser().getId();

        // 对每个用户进行加锁，减少悲观锁的性能影响，因为 synchronized 锁的是当前对象
        // 每个用户就是一个对象，因此每个用户都有自己的锁
        // 但是 toString() 方法底层实际上是重新 new 了一个 String 对象，这样会导致
        // 即使是同一个用户每次进到这个方法依旧会拿到一把锁，跟预期不符
        // 因此后面再加一个 intern() 方法让 String 去常量池找而不是重新 new 一个 String 对象
        synchronized (userId.toString().intern()) {
            // 获取代理对象（事务）
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        // 1.一人一单
        Long userId = UserHolder.getUser().getId();
        // 1.1.查询订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 1.2.判断是否存在
        if (count > 0) {
            return Result.fail("已经下过单了，一个人只能下单一次！");
        }
        // 2.创建订单
        // 2.1.订单 id
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 2.2.用户 id
        voucherOrder.setUserId(userId);
        // 2.3.代金券 id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        // 3.返回订单 id
        return Result.ok(orderId);
    }
}
