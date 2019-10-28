package com.cloud.gateway.config;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.netflix.client.ClientException;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.alibaba.fastjson.JSONObject;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

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
	 * 不处理的话将会抛出500服务端异常，这里只是将下游服务的原始http状态码还原。
	 *
	 * @param exception
	 * @param response
	 * @return
	 */
	@ExceptionHandler({ FeignException.class })
	public Map<String, Object> feignException(FeignException exception, HttpServletResponse response) {
		int httpStatus = exception.status();
		if (httpStatus >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
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
	 * 捕捉 ClientException 异常（IllegalArgumentException）
	 * 不处理的话将会抛出500服务端异常
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

	/**
	 * 捕捉 ClientException 异常（服务的异常和http状态码解析）
	 * 不处理的话将会抛出500服务端异常
	 * @param throwable
	 * @return
	 */
	@ExceptionHandler({ClientException.class, Throwable.class})
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public Map<String, Object> serverException(Throwable throwable) {
		log.error("服务端异常", throwable);
		Map<String, Object> data = new HashMap<>();
		data.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
		data.put("message", "服务端异常，请联系管理员");

		return data;
	}

}
