//package com.cloud.file.config;
//
//import javax.servlet.http.HttpServletResponse;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
//import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
//
//import com.cloud.common.constants.PermitAllUrl;
//
///**
// * 资源服务配置
// *
// *
// * @author admin008
// */
//@EnableResourceServer
//@EnableWebSecurity
//@EnableGlobalMethodSecurity(prePostEnabled = true)
//public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
//
//	/**
//	 * url前缀
//	 */
//	@Value("${file.local.prefix}")
//	public String localFilePrefix;
//
//	/**
//	 * localFilePrefix + "/**"这里将静态资源下的路径放开权限
//	 * @param http
//	 * @throws Exception
//	 */
//	@Override
//	public void configure(HttpSecurity http) throws Exception {
//		http.csrf().disable().exceptionHandling()
//				.authenticationEntryPoint(
//						(request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
//				.and().authorizeRequests()
//				.antMatchers(PermitAllUrl.permitAllUrl("/files-anon/**", localFilePrefix + "/**")).permitAll() // 放开权限的url
//				.anyRequest().authenticated().and().httpBasic();
//	}
//
//}
