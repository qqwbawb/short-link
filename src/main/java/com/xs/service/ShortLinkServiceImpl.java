package com.xs.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xs.common.exception.BizException;
import com.xs.entity.ShortLink;
import com.xs.mapper.ShortLinkMapper;
import com.xs.utils.ShortCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper,ShortLink> implements ShortLinkService {

    @Autowired
    ShortCodeGenerator shortCodeGenerator;

    @Autowired
    RedisTemplate<String,Object> redisTemplate;

    @Override
    public ShortLink createShortLink(String originaurl,Integer expireDay) {
        //生成短码
        String shortCode=shortCodeGenerator.generateShortCode();
        //封装对象
        ShortLink shortLink = new ShortLink();
        shortLink.setShortCode(shortCode);
        shortLink.setOriginalUrl(originaurl);
        shortLink.setClickCount(0L);
        shortLink.setCreateTime(LocalDateTime.now());
        if(expireDay!=null&&expireDay>0){
            shortLink.setExpireTime(LocalDateTime.now().plusDays(expireDay));
        }
        //存入数据库
        this.save(shortLink);
        return shortLink;
    }

    private static final String PREFIX = "shortLink:";

    private static final String COUNT_PREFIX = "shortLink:count:";

    @Override
    public String getOriginalUrl(String shortCode) {
        //先查询redis缓存
        String key=PREFIX+shortCode;
        Object url = redisTemplate.opsForValue().get(key);
        //缓冲里面有，直接返回
        if(url!=null){//需要先判空再转字符串，否则会有空异常
            String code=(String)url;
            if(code.equals("")){
                return null;
            }
            redisTemplate.opsForValue().increment(COUNT_PREFIX+shortCode);
            return code;
        }

        //缓冲里面没有去查数据库
        QueryWrapper<ShortLink> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ShortLink::getShortCode,shortCode);

        ShortLink shortLink = this.getOne(queryWrapper);
        if(shortLink==null){//缓存穿透
            redisTemplate.opsForValue().set(key,"",60,TimeUnit.SECONDS);
            return null;
        }

        //判断链接是否过期
        if (shortLink.getExpireTime()!=null&&shortLink.getExpireTime().isBefore(LocalDateTime.now())){
            redisTemplate.opsForValue().set(key,"",60,TimeUnit.SECONDS);
            return null;
        }
        redisTemplate.opsForValue().set(key,shortLink.getOriginalUrl(),1, TimeUnit.HOURS);
        redisTemplate.opsForValue().increment(COUNT_PREFIX+shortCode);
        //分布式锁，防止重复提交
//        redisTemplate.opsForValue().setIfAbsent(key,shortLink.getClickCount(),1, TimeUnit.MINUTES);
        return shortLink.getOriginalUrl();
    }
}
