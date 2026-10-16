package com.xs.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xs.entity.ShortLink;
import com.xs.mapper.ShortLinkMapper;
import com.xs.utils.ShortCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper,ShortLink> implements ShortLinkService {

    @Autowired
    ShortCodeGenerator shortCodeGenerator;

    @Override
    public ShortLink createShortLink(String originaurl) {
        //生成短码
        String shortCode=shortCodeGenerator.generateShortCode();
        //封装对象
        ShortLink shortLink = new ShortLink();
        shortLink.setShortCode(shortCode);
        shortLink.setOriginalUrl(originaurl);
        shortLink.setClickCount(0L);
        shortLink.setCreateTime(LocalDateTime.now());
        //存入数据库
        this.save(shortLink);
        return shortLink;
    }

    @Override
    public String getOriginalUrl(String shortCode) {
        //构造查询条件
        QueryWrapper<ShortLink> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ShortLink::getShortCode,shortCode);

        ShortLink shortLink = this.getOne(queryWrapper);
        if(shortLink==null){
            return null;
        }
        return shortLink.getOriginalUrl();
    }
}
