package com.xs.dto;

import com.xs.entity.AccessLog;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LinkStatsVo {

    private String shortCode;
    private String originalUrl;
    private Long clickCount;
    private LocalDateTime createTime;
    private LocalDateTime expireTime;

    private List<AccessLog> accessLogs;
}
