package com.ydj.redis.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author yangduojun
 * @date: 2021/11/29
 **/

/**
 * 第8个版本
 * finally块的判断 + del 删除操作不是原子的
 * 解决：用redis自身的事务
 */
@RestController
public class GoodController08 {

    public static final String REDIS_LOCK = "redisLock";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${server.port}")
    private String serverPort;

    @RequestMapping("/buy_goods8")
    public String buy_goods() {

        String value = UUID.randomUUID().toString() + Thread.currentThread().getName();
        try {
            Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(REDIS_LOCK, value, 10L, TimeUnit.SECONDS);

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
            while (true) {
                stringRedisTemplate.watch(REDIS_LOCK);
                if(stringRedisTemplate.opsForValue().get(REDIS_LOCK).equalsIgnoreCase(value)) {
                    stringRedisTemplate.setEnableTransactionSupport(true);
                    stringRedisTemplate.multi();
                    stringRedisTemplate.delete(REDIS_LOCK);
                    List<Object> list = stringRedisTemplate.exec();
                    if(list == null) {
                        continue;
                    }
                }
                stringRedisTemplate.unwatch();
                break;
            }
        }

    }





}
