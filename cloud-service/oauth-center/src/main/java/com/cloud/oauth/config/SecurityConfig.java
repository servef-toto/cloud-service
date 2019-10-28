package com.cloud.oauth.config;

import com.cloud.common.constants.PermitAllUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * spring security配置
 *
 * 主要是用户认证的校验
 * 
 */
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	/**
	 * 用户登录校验的具体实现
	 */
	@Autowired
	public UserDetailsService userDetailsService;
	/**
	 * 密码解析器，用户中心用它加密，所以这里用它解密
	 */
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	/**
	 * 全局用户信息<br>
	 * 方法上的注解@Autowired的意思是，方法的参数的值是从spring容器中获取的<br>
	 * 即参数AuthenticationManagerBuilder是spring中的一个Bean
	 *
	 * @param auth 认证管理
	 * @throws Exception 用户认证异常信息
	 */
	@Autowired
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
	}

	/**
	 * 认证管理
	 * 
	 * @return 认证管理对象
	 * @throws Exception
	 *             认证异常信息
	 */
	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

	/**
	 * http安全配置
	 * 
	 * @param http
	 *            http安全对象
	 * @throws Exception
	 *             http安全异常信息
	 */
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests()

				//"/resources/**", "/signup", "/about" 路径为免验证访问
//				.antMatchers("/resources/**", "/signup", "/about").permitAll()
				//"/admin/**" 路径为 ADMIN 角色可访问
//				.antMatchers("/admin/**").hasRole("ADMIN")
				//"/db/**" 路径为 ADMIN 和 DBA 角色同时拥有时可访问
//				.antMatchers("/db/**").access("hasRole('ADMIN') and hasRole('DBA')")



				// 放开权限的url,不需要登录就可访问
				.antMatchers(PermitAllUrl.permitAllUrl()).permitAll()
				//未匹配路径为登陆可访问
				.anyRequest().authenticated()
				.and()

				.httpBasic() //配置http basic登录
				//Basic Auth是配合RESTful API 使用的最简单的认证方式，只需提供用户名密码即可，
				// 但由于有把用户名密码暴露给第三方客户端的风险，在生产环境下被使用的越来越少。
				// 因此，在开发对外开放的RESTful API时，尽量避免采用Basic Auth。一般Basic验证适用于开发阶段。

				//BasicAuthenticationFilter负责处理HTTPHeader中的基本认证信息。
				//工作原理：在header中获取特定key和特定形式的value（Authorization、Basic [Token]），
				// 获取的到，即使用当前过滤器进行验证身份信息。获取不到，则继续执行正常的过滤链。
				//在使用无状态认证时，需要关闭CSRF.

				.and().csrf().disable();// 使用无状态认证时，需要关闭CSRF
	}



	@Override
	public void configure(WebSecurity web) throws Exception {
		super.configure(web);
	}
}