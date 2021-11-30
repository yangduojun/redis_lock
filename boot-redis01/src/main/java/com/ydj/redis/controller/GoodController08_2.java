package com.ydj.redis.controller;


import com.ydj.redis.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import java.util.Collections;
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
 * 解决：lua脚本
 */
@RestController
public class GoodController08_2 {

    public static final String REDIS_LOCK = "redisLock";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${server.port}")
    private String serverPort;

    @RequestMapping("/buy_goods8_2")
    public String buy_goods() throws Exception {

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
            Jedis jedis = RedisUtils.getJedis();

            String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                    "then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else " +
                    "  return 0 " +
                    "end";

            try {
                Object o = jedis.eval(script, Collections.singletonList(REDIS_LOCK), Collections.singletonList(value));
                if("1".equals(o.toString())) {
                    System.out.println("--------del redis lock ok");
                }else {
                    System.out.println("--------del redis lock error");
                }
            }finally {
                if(null != jedis) {
                    jedis.close();
                }
            }
        }
    }


}
