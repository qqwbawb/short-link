package com.xs.utils;

import com.xs.entity.ShortLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator {

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    public String generateShortCode() {
        Long num = redisTemplate.opsForValue().increment("shortLink:seq");
        StringBuilder sb = new StringBuilder();
        while (num >0) {
            char c;
            int len=(int)(num%62);
            if(len<10){
                c = (char)('0'+len);
            } else if (len<36) {
                c=(char)('a'+len-10);
            }else  {
                c=(char)('A'+len-36);
            }
            num=num/62;
            sb.append(c);
        }
        return sb.reverse().toString();
    }

}
