package com.xs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@TableName("short_link")
public class ShortLink {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String shortCode;
    private String originalUrl;
    private Long clickCount;
    private LocalDateTime createTime;
    private LocalDateTime expireTime;

    @TableField(exist = false)
    private String shortLink;
}
