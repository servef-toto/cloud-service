package com.cloud.backend.config;

import com.alibaba.fastjson.JSONObject;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一异常处理
 * @ControllerAdvice是组件注解，他使得其实现类能够被classpath扫描自动发现，类似于配置注解configration能被springcontext发现
 * 注解@ControllerAdvice的类可以拥有@ExceptionHandler, @InitBinder或 @ModelAttribute注解的方法.
 * 并且这些方法会被应用到控制器类层次的所有@RequestMapping方法上。
 */
@Slf4j
@RestControllerAdvice
public class ExceptionHandlerAdvice {

    /**
     * feignClient调用异常，将服务的异常和http状态码解析
     *
     * @param exception
     * @param response
     * @return
     */
    @ExceptionHandler({ FeignException.class })
    public Map<String, Object> feignException(FeignException exception, HttpServletResponse response) {
        int httpStatus = exception.status();
        if (httpStatus >= 500) {
            log.error("feignClient调用异常", exception);
        }

        Map<String, Object> data = new HashMap<>();

        String msg = exception.getMessage();

        if (!StringUtils.isEmpty(msg)) {
            int index = msg.indexOf("\n");
            if (index > 0) {
                String string = msg.substring(index);
                if (!StringUtils.isEmpty(string)) {
                    JSONObject json = JSONObject.parseObject(string.trim());
                    data.putAll(json.getInnerMap());
                }
            }
        }
        if (data.isEmpty()) {
            data.put("message", msg);
        }
        data.put("code", httpStatus + "");
        response.setStatus(httpStatus);
        return data;
    }

    /**
     * ExceptionHandler统一异常处理，这里统一处理IllegalArgumentException异常
     * 并且ResponseStatus注解表示当异常出现时，使用这里指明的 error code 和 error reasoon 返回给客户端
     * @param exception
     * @return
     */
    @ExceptionHandler({ IllegalArgumentException.class })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> badRequestException(IllegalArgumentException exception) {
        Map<String, Object> data = new HashMap<>();
        data.put("code", HttpStatus.BAD_REQUEST.value());
        data.put("message", exception.getMessage());

        return data;
    }
}
