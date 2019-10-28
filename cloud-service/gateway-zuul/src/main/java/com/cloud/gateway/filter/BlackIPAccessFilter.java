package com.cloud.gateway.filter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cloud.gateway.feign.BackendClient;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

/**
 * 黑名单IP拦截<br>
 * 黑名单ip变化不会太频繁，<br>
 * 考虑到性能，我们不实时掉接口从别的服务获取了，<br>
 * 而是定时把黑名单ip列表同步到网关层,
 *
 */
@Component
public class BlackIPAccessFilter extends ZuulFilter {

	/**
	 * 黑名单列表
	 */
	private Set<String> blackIPs = new HashSet<>();

	/**
	 * shouldFilter：判断该过滤器是否需要被执行。
	 * true： 过滤器对请求都生效,就是执行完当前过滤器,才会继续往下执行
	 * false: 过滤器对请求不生效,就是之间往下执行，不执行当前过滤器
	 *
	 * 这里对黑名单ip进行校验，如果是黑名单ip集合中的，就不能往下游放。
	 * @return
	 */
	@Override
	public boolean shouldFilter() {
		if (blackIPs.isEmpty()) {
			return false;
		}

		RequestContext requestContext = RequestContext.getCurrentContext();
		HttpServletRequest request = requestContext.getRequest();
		String ip = getIpAddress(request);

		return blackIPs.contains(ip);// 判断ip是否在黑名单列表里
	}

	/**
	 * run：过滤器的具体逻辑。
	 * 这里我们通过使用requestContext.setResponseBody(body)对返回的body内容进行编辑返回提示信息。
	 * 然后也通过requestContext.setSendZuulResponse(false)令zuul过滤该请求，不对其进行路由，
	 * @return
	 */
	@Override
	public Object run() {
		RequestContext requestContext = RequestContext.getCurrentContext();
		requestContext.setResponseStatusCode(HttpStatus.FORBIDDEN.value());
		requestContext.setResponseBody("black ip");
		requestContext.setSendZuulResponse(false);

		return null;
	}

	/**
	 * filterOrder：过滤器的执行顺序。当请求在一个阶段中存在多个过滤器时，需要根据该方法返回都值来依次执行。
	 * @return
	 */
	@Override
	public int filterOrder() {
		return 0;
	}

	/**
	 * filterType：过滤器都类型，它决定过滤器在请求都哪一个生命周期中执行。这里定义为pre，代表会在请求被路由之前执行。
	 * @return
	 */
	@Override
	public String filterType() {
		return FilterConstants.PRE_TYPE;
	}




	@Autowired
	private BackendClient backendClient;

	/**
	 * 定时同步黑名单IP
	 */
	@Scheduled(cron = "${cron.black-ip}")
	public void syncBlackIPList() {
		try {
			Set<String> list = backendClient.findAllBlackIPs(Collections.emptyMap());
			blackIPs = list;
		} catch (Exception e) {
			// do nothing
		}
	}

	/**
	 * 获取请求的真实ip
	 * 
	 * @param request
	 * @return
	 */
	public static String getIpAddress(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_CLIENT_IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}

}
