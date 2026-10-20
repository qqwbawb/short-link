package com.xs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xs.entity.AccessLog;


public interface AccessLogService extends IService<AccessLog> {
    //保存访问日志
    void saveAccessLog(String shortCode, String ip, String userAgent);

}
