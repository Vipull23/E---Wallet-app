package org.gfg.NotificationService.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisUtil {

    @Autowired
    RedisTemplate<String,String> redisTemplate;

    public void setData(String key, String value){
        redisTemplate.opsForValue().set(key,value);
    }

    public String getData(String key){
        return redisTemplate.opsForValue().get(key);
    }

}
