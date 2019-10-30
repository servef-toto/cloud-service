package com.cloud.gateway.config;

import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * spring security配置
 * @EnableOAuth2Sso,这个注解会帮我们完成跳转到授权服务器,当然要些配置application.yml
 *
 */
@EnableOAuth2Sso
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
        // 使用无状态认证时，需要关闭CSRF
		http.csrf().disable();

        // 解决在项目里引入Spring Security后iframe或者frame所引用的页无法显示的问题
        // 出现这个问题的原因是因为Spring Security默认将header response里的X-Frame-Options属性设置为DENY。
        // 如果页面里有需要通过iframe/frame引用的页面，需要配置Spring Security允许iframe frame加载同源的资源，
        // 方法为在Spring Security的配置类里将header response的X-Frame-Options属性设置为SAMEORIGIN
		http.headers().frameOptions().sameOrigin();

		// 解决跨域问题
		http.cors();


//		http.requestMatchers().antMatchers("/login")
//				.and().authorizeRequests()
//				.antMatchers("/login").permitAll();
	}

}