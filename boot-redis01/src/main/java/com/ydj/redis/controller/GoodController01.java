package com.ydj.redis.controller;

/**
 * @author yangduojun
 * @date: 2021/11/29
 **/

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 第一个版本
 * 单机版
 */
public class GoodController01 {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${server.port}")
    private String serverPort;

    @RequestMapping("/buy_goods1")
    public String buy_goods() {
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
    }
}
