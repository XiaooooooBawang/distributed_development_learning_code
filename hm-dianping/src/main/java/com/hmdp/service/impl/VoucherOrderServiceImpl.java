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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.hmdp.utils.RedisConstants.SECKILL_ORDER_KEY;
import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
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
     * 创建阻塞队列，当阻塞队列为空或满时，会阻塞，除此之外才会被唤醒
     */
    private final BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);

    //异步处理线程池，这里用单线程的线程池就好了，慢慢来，不着急
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    //获取代理对象（事务）,由于spring的事务是放在threadLocal中，此时的是多线程，事务会失效，所以只能放在成员变量当中
    private IVoucherOrderService proxy;


    //在类初始化之后执行，因为当这个类初始化好了之后，随时都是有可能要执行的
    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    /**
     * 内部类
     * 用于线程池处理的任务
     * 当初始化完毕后，就会去从对列中去拿信息
     */
    private class VoucherOrderHandler implements Runnable {
        @Override
        public void run() {
            while (true) {

                try {
                    // 1.获取队列中的订单信息,take是阻塞地拿
                    VoucherOrder voucherOrder = orderTasks.take();
                    // 2.创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("处理订单异常：", e);
                }
            }
        }

        /**
         * 异步处理订单
         *
         * @param voucherOrder
         */
        private void handleVoucherOrder(VoucherOrder voucherOrder) {
            //获取用户id，因为是从线程池中拿到的子线程，非主线程，所以不能在threadlocal中取用户id，只能在voucherOrder中取
            Long userId = voucherOrder.getUserId();
            //创建锁对象
            //创建redisson中普通的锁，但是是可重入的
            RLock lock = redissonClient.getLock("lock:order:" + userId);

            //获取锁对象
            boolean isSuccess = lock.tryLock();
            //若获取锁失败
            if (!isSuccess) {
                log.error("一人只能下一单");
                return;
            }
            //若获取锁成功
            try {
                //调用代理对象的方法,这个对象需要是IVoucherOrderService里的接口
                proxy.createVoucherOrder(voucherOrder);
            } finally {
                //释放锁
                //使用lua脚本来释放锁，实现获取锁、比较锁和删除锁的操作的原子性，防止比较锁和删除锁之间出现阻塞，导致多线程情况下出现误删锁
                //lock.unlockWithLua();  //自己写的释放锁
                lock.unlock();
            }
        }
    }


    @Override
    public Result seckillVoucherWithBlockingQueue(Long voucherId) {
        //获取用户id，这里还是主线程，所以可以从threadlocal拿
        Long userId = UserHolder.getUser().getId();
        long orderId = redisIdWorker.nextId("order");
        // 1.执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Arrays.asList(SECKILL_STOCK_KEY,SECKILL_ORDER_KEY),
                voucherId.toString(), userId.toString(), String.valueOf(orderId)
        );
        int r = Objects.requireNonNull(result).intValue();
        // 2.判断结果是否为0
        if (r != 0) {
            // 2.1.不为0 ，代表没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        //2.2 为0，有购买资格，把下单信息保存到阻塞队列中
        //3.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 3.1.订单id
        voucherOrder.setId(orderId);
        // 3.2.用户id
        voucherOrder.setUserId(userId);
        // 3.3.代金券id
        voucherOrder.setVoucherId(voucherId);
        // 3.4 把下单信息保存到阻塞队列中，剩下就异步去处理阻塞队列里的订单
        orderTasks.add(voucherOrder);
        // 4.获取代理对象
        proxy = (IVoucherOrderService)AopContext.currentProxy();
        // 5.返回订单id
        return Result.ok(orderId);
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1.查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2.判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            // 尚未开始
            return Result.fail("秒杀尚未开始");
        }
        // 3.判断秒杀是否已经结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            // 已结束
            return Result.fail("秒杀已结束");
        }
        // 4.判断库存是否充足
        if (voucher.getStock() < 1) {
            // 库存不足
            return Result.fail("库存不足");
        }

        //用户id
        Long userId = UserHolder.getUser().getId();

        /*
        redis实现分布式锁
         */
        //创建锁对象
//        SimpleRedisLock lock = new SimpleRedisLock(stringRedisTemplate, "order:" + userId);//自己写的锁
        //创建redisson中普通的锁，但是是可重入的
        RLock lock = redissonClient.getLock("lock:order:" + userId);

        //获取锁对象
        //超时时间应该根据正常业务的执行时间来选择，一般选择10倍正常业务的执行时间，这里为了方便打断点调试，设置大点
        //boolean isSuccess = lock.getLock(1200);  //自己写的尝试获取锁
        boolean isSuccess = lock.tryLock();
        //若获取锁失败
        if (!isSuccess) {
            return Result.fail("一人只能下一单");
        }
        //若获取锁成功
        try {
            //获取代理对象（事务）
            //IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            //调用代理对象的方法,这个对象需要是IVoucherOrderService里的接口
            //return proxy.createVoucherOrder(voucherId);
            return null;
        } finally {
            //释放锁
            //使用lua脚本来释放锁，实现获取锁、比较锁和删除锁的操作的原子性，防止比较锁和删除锁之间出现阻塞，导致多线程情况下出现误删锁
            //lock.unlockWithLua();  //自己写的释放锁
            lock.unlock();
        }



        /*
         * 1.乐观锁比较适合更新数据，而现在是插入数据，所以我们需要使用悲观锁操作。
         * 2.当前方法被spring的事务控制，如果你在方法内部加锁，可能会导致当前方法事务还没有提交，但是锁已经释放也会导致问题，
         * 所以我们选择将当前方法整体包裹起来，确保事务不会出现问题。
         * 3.intern() 这个方法是从常量池中拿到数据，如果我们直接使用userId.toString()，
         * 他拿到的对象实际上是不同的对象，new出来的对象，我们使用锁必须保证锁必须是同一把，所以我们需要使用intern()。
         * 4.事务想要生效，还得利用代理对象来生效，直接调用的话其实就是调用了this.createVoucherOrder，
         * 而this本身是不被spring事务管理的，所以会事务注解失效。spring管理事务是用代理对象实现的，
         * 所以需要获取当前对象的代理对象，再调用createVoucherOrder接口，
         * 同时需要另外导入aspectJ的依赖包，并在入口类中设置暴露代理对象，不然拿不到
         */
//        synchronized (userId.toString().intern()) {
//            //获取代理对象（事务）
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            //调用代理对象的方法,这个对象需要是IVoucherOrderService里的接口
//            return proxy.createVoucherOrder(voucherId);
//        }
    }

    @Override
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        // 5.一人一单逻辑
        //5.1获取用户id，因为是从线程池中拿到的子线程，非主线程，所以不能在threadlocal中取用户id，只能在voucherOrder中取
        Long userId = voucherOrder.getUserId();
        // 5.2.判断是否存在
        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();

        if (count > 0) {
            // 用户已经购买过了
            log.error("一人只能下一单");
        }
        //6.扣减库存，加CAS乐观锁解决线程安全问题
        boolean isSuccess = seckillVoucherService.update()
                .setSql("stock = stock - 1")//set stock = stock -1
                .eq("voucher_id", voucherOrder.getVoucherId())
                // eq("stock", voucher.getStock())
                // where id = ？ and stock = ? 这种乐观锁有弊端，秒杀的业务逻辑上不要求这个数据必须没改过，所以可以用下面这种只要还有库存就可以减
                .gt("stock", 0)
                //where id = ? and stock > 0 在stock 大于1的情况下是没用到乐观锁的思想的，用的是mysql执行更新时会加行锁，只有在剩下1个stock的时候只有一个人能操作成功，就体现了乐观锁的思想
                .update();
        if (!isSuccess) {
            //扣减失败
            log.error("库存不足");
        }
        save(voucherOrder);
    }
}
