package com.xs.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class CreateLinkRequest {

    @NotBlank(message = "原始链接不能为空")
    @Size(max = 2048, message = "链接长度不能超过2048")
    @Pattern(regexp = "^https?://.*",message = "链接必须以http/https开头")
    private String originalUrl;

    private Integer expireDay;

}
