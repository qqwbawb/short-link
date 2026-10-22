package com.xs.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xs.entity.ShortLink;
import com.xs.service.ShortLinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class ClickCountSyncTask {

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Autowired
    private ShortLinkService shortLinkService;

    private static final String COUNT_PREFIX = "shortLink:count:";

    @Scheduled(fixedRate = 5*60*1000)
    public void syncClickCount(){
        Set<String> keys=redisTemplate.keys(COUNT_PREFIX+"*");
        if(keys==null||keys.size()==0){
            return;
        }

        int synced=0;
        for(String key:keys){
            //获取点击次数
            Object value=redisTemplate.opsForValue().get(key);
            if(value==null){
                continue;
            }
            Long count=Long.parseLong(value.toString());
            redisTemplate.delete(key);//获取点击次数后删除
            if(count<=0){
                continue;
            }

            //查询数据库
            String shortCode= key.substring(COUNT_PREFIX.length());
            QueryWrapper<ShortLink> queryWrapper=new QueryWrapper<>();
            queryWrapper.lambda().eq(ShortLink::getShortCode,shortCode);
            ShortLink shortLink=shortLinkService.getOne(queryWrapper);
            if(shortLink==null){
                continue;
            }

            Long old=shortLink.getClickCount()==null?0:shortLink.getClickCount();
            shortLink.setClickCount(old+count);
            shortLinkService.updateById(shortLink);
            synced++;
        }

        log.info("点击落库完成，同步了{ }条"+synced);
    }
}
