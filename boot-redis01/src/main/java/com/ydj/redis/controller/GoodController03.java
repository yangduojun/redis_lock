package com.ydj.redis.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * @author yangduojun
 * @date: 2021/11/28
 **/

/**
 * 第3个版本
 * redis分布式锁setnx
 */
@RestController
public class GoodController03 {

    public static final String REDIS_LOCK = "redisLock";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${server.port}")
    private String serverPort;

    @RequestMapping("/buy_goods3")
    public String buy_goods() {

        String value = UUID.randomUUID().toString() + Thread.currentThread().getName();
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(REDIS_LOCK, value);

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
            stringRedisTemplate.delete(REDIS_LOCK);
            return "成功买到商品，库存还剩下：" + realNum + " 件\t服务提供端口 " + serverPort;
        }else {
            System.out.println("商品已经售完/活动结束/调用超时，欢迎下次光临\t服务提供端口 " + serverPort);

        }

        return "商品已经售完/活动结束/调用超时，欢迎下次光临\t服务提供端口 " + serverPort;
    }





}
