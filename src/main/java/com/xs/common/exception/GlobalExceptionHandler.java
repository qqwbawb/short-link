package com.xs.common.exception;

import com.xs.common.utils.ResultUtils;
import com.xs.common.utils.ResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    //业务异常，可以预测，只记warn
    @ExceptionHandler(value = BizException.class)
    public ResultVo handleBizException( BizException e ) {
        log.warn("业务异常：{}", e.getMessage() );
        return ResultUtils.error(e.getMessage(), e.getCode());
    }

    //参数校验异常
    @ExceptionHandler(value = {MethodArgumentNotValidException.class, BindException.class})
    public ResultVo handleValidException(Exception e){
        String message = "参数校验失败";
        if(e instanceof MethodArgumentNotValidException){
            FieldError fieldError = ((MethodArgumentNotValidException) e).getBindingResult().getFieldError();
            if(fieldError != null){
                message = fieldError.getDefaultMessage();
            }
        }
        log.warn("参数校验失败：{}", message);
        return ResultUtils.error(message,400);
    }

    //未预计异常，返回堆栈
    @ExceptionHandler(value = Exception.class)
    public ResultVo handleException(Exception e){
        log.error("系统错误:", e);
        return ResultUtils.error("系统繁忙，请稍后重试", 500);
    }
}
