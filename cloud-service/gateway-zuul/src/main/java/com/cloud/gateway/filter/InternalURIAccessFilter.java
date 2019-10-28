package com.cloud.gateway.filter;

import javax.servlet.http.HttpServletRequest;

import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

/**
 * 过滤uri<br>
 * 该类uri不需要登陆，但又不允许外网通过网关调用，只允许微服务间在内网调用，<br>
 * 为了方便拦截此场景的uri，我们自己约定一个规范，uri中含有-anon/internal<br>
 * 如在oauth登陆的时候用到根据username查询用户，<br>
 * 用户系统提供的查询接口/users-anon/internal肯定不能做登录拦截，而该接口也不能对外网暴露<br>
 * 如果有此类场景的uri，请用这种命名格式，
 *
 *
 */
@Component
public class InternalURIAccessFilter extends ZuulFilter {
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
		requestContext.setResponseBody(HttpStatus.FORBIDDEN.getReasonPhrase());
		requestContext.setSendZuulResponse(false);

		return null;
	}

	/**
	 * shouldFilter：判断该过滤器是否需要被执行。
	 * true： 过滤器对请求都生效,就是执行完当前过滤器,才会继续往下执行
	 * false: 过滤器对请求不生效,就是之间往下执行，不执行当前过滤器
	 *
	 * 这个对请求url进行校验，url中能匹配*-anon/internal* 说明不能被外网访问，不往下进行路由
	 * @return
	 */
	@Override
	public boolean shouldFilter() {
		RequestContext requestContext = RequestContext.getCurrentContext();
		HttpServletRequest request = requestContext.getRequest();

		return PatternMatchUtils.simpleMatch("*-anon/internal*", request.getRequestURI());
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

}
