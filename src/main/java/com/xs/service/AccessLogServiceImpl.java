package com.xs.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xs.entity.AccessLog;
import com.xs.mapper.AccessLogMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
}
