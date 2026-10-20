package com.xs.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xs.common.utils.ResultUtils;
import com.xs.common.utils.ResultVo;
import com.xs.entity.ShortLink;
import com.xs.service.AccessLogService;
import com.xs.service.ShortLinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.PathParam;
import java.io.IOException;

@RestController
public class ShortLinkController {

    @Autowired
    private ShortLinkService shortLinkService;

    @PostMapping("/api/link")
    public ResultVo<ShortLink> createShortLink(@RequestBody ShortLink param) {
        //查询原链是否有未过期短链
        QueryWrapper<ShortLink> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ShortLink::getOriginalUrl, param.getOriginalUrl());
        ShortLink one = shortLinkService.getOne(queryWrapper);
        if(one!=null){
            return ResultUtils.error("生成错误，已有短链");
        }
        ShortLink shortLink = shortLinkService.createShortLink(param.getOriginalUrl());
        shortLink.setShortLink("http://localhost:8090/s/"+shortLink.getShortCode());
        return ResultUtils.success("生成成功",shortLink);
    }

    @Autowired
    private AccessLogService accessLogService;

    @GetMapping("/s/{code}")
    public void getShortLink(@PathVariable("code") String shortCode, HttpServletRequest request, HttpServletResponse response) throws IOException {

        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("user-agent");

        String one = shortLinkService.getOriginalUrl(shortCode);
        if(one==null){
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("短链不存在");
            return;
        }

        accessLogService.saveAccessLog(shortCode,ip,userAgent);

        response.sendRedirect(one);
    }
}
