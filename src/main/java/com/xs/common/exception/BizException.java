package com.xs.common.exception;

import lombok.Getter;

/*
自定义的业务异常
 */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(String message) {
        super(message);
        this.code=500;
    }

    public BizException(int code, String message) {
        super(message);
        this.code=code;
    }
}
