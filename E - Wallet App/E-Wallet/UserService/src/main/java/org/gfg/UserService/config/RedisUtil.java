package org.gfg.UserService.config;

import org.gfg.UserService.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisUtil {

    @Autowired
    RedisTemplate<String, User> redisTemplate;


    @Autowired
    @Qualifier("otpRedis")
    RedisTemplate<String,String> otpRedis;

    public void setData(String key, User value){
        redisTemplate.opsForValue().set(key,value);
    }

    public User getData(String key){
        return redisTemplate.opsForValue().get(key);
    }

    public String getOTP(String key){
        return otpRedis.opsForValue().get(key);
    }

}
