package com.xs.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xs.common.exception.BizException;
import com.xs.dto.LinkStatsVo;
import com.xs.entity.AccessLog;
import com.xs.entity.ShortLink;
import com.xs.mapper.AccessLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccessLogServiceImpl extends ServiceImpl<AccessLogMapper,AccessLog> implements AccessLogService {
    @Async
    @Override
    public void saveAccessLog(String shortCode, String ip, String userAgent) {
        AccessLog accessLog = new AccessLog();
        accessLog.setShortCode(shortCode);
        accessLog.setIp(ip);
        accessLog.setUserAgent(userAgent);
        accessLog.setAccessTime(LocalDateTime.now());
        this.baseMapper.insert(accessLog);
    }

    @Autowired
    private ShortLinkService shortLinkService;

    @Override
    public LinkStatsVo getLinkStats(String shortCode) {
        QueryWrapper<ShortLink>  queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ShortLink::getShortCode, shortCode);
        ShortLink link=shortLinkService.getOne(queryWrapper);
        if(link==null){
            throw new BizException("短链不存在");
        }
        LinkStatsVo vo=new LinkStatsVo();
        vo.setShortCode(link.getShortCode());
        vo.setOriginalUrl(link.getOriginalUrl());
        vo.setClickCount(link.getClickCount());
        vo.setCreateTime(link.getCreateTime());
        vo.setExpireTime(link.getExpireTime());

        QueryWrapper<AccessLog>  queryWrapper2 = new QueryWrapper<>();
        queryWrapper2.lambda().eq(AccessLog::getShortCode,link.getShortCode())
                .orderByDesc(AccessLog::getAccessTime)
                .last("limit 10");
        vo.setAccessLogs(baseMapper.selectList(queryWrapper2));
        return vo;
    }
}
