package com.xs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xs.entity.ShortLink;

public interface ShortLinkService extends IService<ShortLink> {

    //生成短码并保存
    ShortLink createShortLink(String originaurl);

    //根据短码查原链
    String getOriginalUrl(String shortCode);
}
