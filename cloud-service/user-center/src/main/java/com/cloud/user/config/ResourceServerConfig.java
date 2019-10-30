package com.cloud.user.config;

import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;

import com.cloud.common.constants.PermitAllUrl;

/**
 * 资源权限配置
 *
 *  @EnableResourceServer将我们的项目作为资源服务器
 *  @EnableWebSecurity将开启认证校验
 *  @EnableGlobalMethodSecurity(prePostEnabled = true)是启动权限注解支持
 *
 * antMatchers(***).permitAll() 这里符合规则的url将不做权限拦截。
 *
 * @author admin008
 */
@EnableResourceServer
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
	/**
	 * 该方法主要用来配置路径拦截规则
	 * @param http
	 * @throws Exception
	 */
	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.csrf().disable()
				// 失败处理，这一句表示权限不足或未登录时，报一个HttpServletResponse.SC_UNAUTHORIZED（401）状态码
				.exceptionHandling().authenticationEntryPoint((request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))

				.and().authorizeRequests()
				// 不需要做权限规则的url
				.antMatchers(PermitAllUrl.permitAllUrl("/users-anon/**",
                        "/wechat/**",
                        "/open/**")).permitAll() // 放开权限的url
				// 这里其他的路径都需要拦截认证
				.anyRequest().authenticated()
				.and().httpBasic();
	}

	/**
	 * 密码加密处理器
	 * @return
	 */
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
