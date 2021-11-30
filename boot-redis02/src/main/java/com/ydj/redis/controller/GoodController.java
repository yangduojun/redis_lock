package com.ydj.redis.controller;


import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * @author yangduojun
 * @date: 2021/11/29
 **/

/**
 * 第9个版本
 * 确保RedisLock过期时间大于业务执行时间的问题(redis分布式锁如何续期)
 *
 */
@RestController
public class GoodController {

    public static final String REDIS_LOCK = "redisLock";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${server.port}")
    private String serverPort;

    @Autowired
    private Redisson redisson;

    @RequestMapping("/buy_goods")
    public String buy_goods() throws Exception {

        String value = UUID.randomUUID().toString() + Thread.currentThread().getName();
        RLock redissonLock = redisson.getLock(REDIS_LOCK);
        redissonLock.lock();

        try {

            // get key ==>  看看库存的数量够不够
            String result = stringRedisTemplate.opsForValue().get("goods:001");
            int goodsNum = result == null ? 0 : Integer.parseInt(result);

            if(goodsNum > 0) {
                //真实库存
                int realNum = goodsNum - 1;
                stringRedisTemplate.opsForValue().set("goods:001", String.valueOf(realNum));
                System.out.println("成功买到商品，库存还剩下：" + realNum + " 件\t服务提供端口 " + serverPort);
                return "成功买到商品，库存还剩下：" + realNum + " 件\t服务提供端口 " + serverPort;
            }else {
                System.out.println("商品已经售完/活动结束/调用超时，欢迎下次光临\t服务提供端口 " + serverPort);

            }

            return "商品已经售完/活动结束/调用超时，欢迎下次光临\t服务提供端口 " + serverPort;
        }finally {
            if(redissonLock.isLocked() && redissonLock.isHeldByCurrentThread()) {
                redissonLock.unlock();
            }

        }
    }


}
