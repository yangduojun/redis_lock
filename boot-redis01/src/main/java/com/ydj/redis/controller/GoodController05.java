package com.ydj.redis.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author yangduojun
 * @date: 2021/11/29
 **/

/**
 * 第5个版本
 * 部署了微服务jar包的机器挂了，代码层面根本没有走到finally这块，没办法保证解锁，这个key没有被删除，需要加入一个过期时间限定
 * 解决：需要对lockKey有过期时间的限定
 */
@RestController
public class GoodController05 {

    public static final String REDIS_LOCK = "redisLock";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${server.port}")
    private String serverPort;

    @RequestMapping("/buy_goods5")
    public String buy_goods() {

        String value = UUID.randomUUID().toString() + Thread.currentThread().getName();
        try {
            Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(REDIS_LOCK, value);
            stringRedisTemplate.expire(REDIS_LOCK, 10L, TimeUnit.SECONDS);

            if (!flag) {
                return "抢锁失败";
            }

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
            stringRedisTemplate.delete(REDIS_LOCK);
        }

    }





}
