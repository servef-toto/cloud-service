package com.cloud.backend.config;

import com.cloud.common.constants.PermitAllUrl;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;

import javax.servlet.http.HttpServletResponse;

/**
 * 资源权限配置
 *
 *  @EnableResourceServer将我们的项目作为资源服务器
 *  @EnableWebSecurity将开启认证校验
 *  @EnableGlobalMethodSecurity(prePostEnabled = true)是启动权限注解支持
 *
 *  antMatchers(***).permitAll() 这里符合规则的url将不做权限拦截。
 */
@EnableResourceServer
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                // 失败处理，这一句表示权限不足或未登录时，报一个HttpServletResponse.SC_UNAUTHORIZED（401）状态码
                .exceptionHandling().authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                .and().authorizeRequests()
                // 不需要做权限规则的url
                .antMatchers(PermitAllUrl.permitAllUrl("/backend-anon/**", "/favicon.ico", "/css/**", "/js/**",
                        "/fonts/**", "/layui/**", "/img/**", "/pages/**", "/pages/**/*.html", "/*.html")).permitAll() // 放开权限的url
                // 这里其他的路径都需要拦截认证
                .anyRequest().authenticated()

                .and().httpBasic();//配置http basic登录
                                    //Basic Auth是配合RESTful API 使用的最简单的认证方式，只需提供用户名密码即可，
                                    // 但由于有把用户名密码暴露给第三方客户端的风险，在生产环境下被使用的越来越少。
                                    // 因此，在开发对外开放的RESTful API时，尽量避免采用Basic Auth。一般Basic验证适用于开发阶段。

                                    //BasicAuthenticationFilter负责处理HTTPHeader中的基本认证信息。
                                    //工作原理：在header中获取特定key和特定形式的value（Authorization、Basic [Token]），
                                    // 获取的到，即使用当前过滤器进行验证身份信息。获取不到，则继续执行正常的过滤链。
                                    //在使用无状态认证时，需要关闭CSRF.

        // 解决在项目里引入Spring Security后iframe或者frame所引用的页无法显示的问题
        // 出现这个问题的原因是因为Spring Security默认将header response里的X-Frame-Options属性设置为DENY。
        // 如果页面里有需要通过iframe/frame引用的页面，需要配置Spring Security允许iframe frame加载同源的资源，
        // 方法为在Spring Security的配置类里将header response的X-Frame-Options属性设置为SAMEORIGIN
        http.headers().frameOptions().sameOrigin();
    }

}
