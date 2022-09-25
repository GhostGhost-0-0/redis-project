package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @BelongProject: hm-dianping
 * @BelongPackage: com.hmdp.service.impl
 * @Author: 那个小楠瓜
 * @CreateTime: 2022-09-19 21:15
 * @Description: 优惠券的订单表服务实现类
 * @Version: 1.0
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /**
     * 阻塞队列
     */
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);

    /**
     * 异步处理线程池
     */
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    /**
     * 在类初始化之后执行，因为当这个类初始化好了之后，随时都是有可能要执行的
     */
    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    /**
     * 用于线程池处理的任务
     * 当初始化完毕后，就会去从队列中去拿信息
     */
    private class VoucherOrderHandler implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    // 1.获取队列中的订单信息
                    VoucherOrder order = orderTasks.take();
                    // 2.创建订单
                    handleVoucherOrder(order);
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                }
            }
        }
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        // 1.获取用户
        Long userId = voucherOrder.getUserId();
        // 2.创建锁对象
        RLock lock = redissonClient.getLock("orders" + userId);
        // 3.获取锁
        boolean isLock = lock.tryLock();
        // 4.判断是否获取锁成功
        if (!isLock) {
            log.error("不允许重复下单");
            return;
        }
        // 5.创建订单
        try {
            // 获取代理对象（事务）
            // 注意：由于是 spring 的事务是放在 threadLocal 中，此时是多线程，事务会失效
            // 因此要在外部先定义一个成员变量把 voucherOrderServiceImpl 代理先拿到
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    /**
     * 父线程的代理对象
     */
    private IVoucherOrderService proxy;

    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        // 1.执行 Lua 脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString());
        // 2.判断结果是否为 0
        int r = result.intValue();
        if (r != 0) {
            // 2.1.不为0，代表没有购买资格
            return Result.fail(r == 1 ? "库存不足！" : "不能重复下单");
        }
        // 2.2.为0，有购买资格，把下单信息保存到阻塞队列
        VoucherOrder voucherOrder = new VoucherOrder();
        // 2.3.订单 id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 2.4.用户 id
        voucherOrder.setUserId(userId);
        // 2.5.代金券 id
        voucherOrder.setVoucherId(voucherId);
        // 2.6.放入阻塞队列
        orderTasks.add(voucherOrder);
        // 3.获取代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        // 3.返回订单 id
        return Result.ok(orderId);
    }

    /*@Override
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
        // 5.创建订单
        // 5.1.创建锁对象
        Long userId = UserHolder.getUser().getId();
        //SimpleRedisLock lock = new SimpleRedisLock("orders:" + userId, stringRedisTemplate);
        // 使用 redisson 获取锁对象
        RLock lock = redissonClient.getLock("orders" + userId);
        // 5.2.获取锁
        boolean isLock = lock.tryLock();
        // 5.3.判断是否获取锁成功
        if (!isLock) {
            // 获取锁失败，返回错误信息或重试
            return Result.fail("不允许重复下单");
        }
        // 5.4.创建订单
         try {
            // 获取代理对象（事务）
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
             // 释放锁
             lock.unlock();
         }
    }*/

    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        // 1.一人一单
        Long userId = voucherOrder.getUserId();
        // 1.1.查询订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        // 1.2.判断是否存在
        if (count > 0) {
            log.error("用户已经购买过了");
            return;
        }
        // 2.扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1") // set stock = stock - 1
                // 使用乐观锁解决超卖线程安全问题
                .eq("voucher_id", voucherOrder.getVoucherId()).gt("stock", 0) // where voucher_id = ? and stock > 0
                .update();
        if (!success) {
            // 扣减失败
            log.error("库存不足!");
            return;
        }
        save(voucherOrder);
    }

    /*@Transactional
    public Result createVoucherOrder(Long voucherId) {
        // 1.一人一单
        Long userId = UserHolder.getUser().getId();
        // 1.1.查询订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 1.2.判断是否存在
        if (count > 0) {
            return Result.fail("已经下过单了，一个人只能下单一次！");
        }
        // 2.扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1") // set stock = stock - 1
                // 使用乐观锁解决超卖线程安全问题
                .eq("voucher_id", voucherId).gt("stock", 0) // where voucher_id = ? and stock > 0
                .update();
        if (!success) {
            // 扣减失败
            return Result.fail("库存不足！");
        }
        // 3.创建订单
        // 3.1.订单 id
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 3.2.用户 id
        voucherOrder.setUserId(userId);
        // 3.3.代金券 id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        // 4.返回订单 id
        return Result.ok(orderId);
    }*/
}
