package com.cloud.notification.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一异常处理
 * @ControllerAdvice是组件注解，他使得其实现类能够被classpath扫描自动发现，类似于配置注解configration能被springcontext发现
 * 注解@ControllerAdvice的类可以拥有@ExceptionHandler, @InitBinder或 @ModelAttribute注解的方法.
 * 并且这些方法会被应用到控制器类层次的所有@RequestMapping方法上。
 */
@RestControllerAdvice
public class ExceptionHandlerAdvice {

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
